package com.pharmacy.demo.service.impl;

import com.mongodb.MongoException;
import com.pharmacy.demo.dto.request.CreateInvoiceRequest;
import com.pharmacy.demo.dto.request.InvoiceItemRequest;
import com.pharmacy.demo.dto.response.CodePreviewResponse;
import com.pharmacy.demo.dto.response.InvoiceDetailResponse;
import com.pharmacy.demo.entity.Customer;
import com.pharmacy.demo.entity.Invoice;
import com.pharmacy.demo.entity.InvoiceDraft;
import com.pharmacy.demo.entity.Medicine;
import com.pharmacy.demo.entity.embedded.BatchAllocation;
import com.pharmacy.demo.entity.embedded.InvoiceLine;
import com.pharmacy.demo.enums.BatchHandlingStatus;
import com.pharmacy.demo.enums.DiscountType;
import com.pharmacy.demo.enums.InvoiceStatus;
import com.pharmacy.demo.enums.MedicineStatus;
import com.pharmacy.demo.enums.MovementType;
import com.pharmacy.demo.enums.PaymentMethod;
import com.pharmacy.demo.enums.PrescriptionType;
import com.pharmacy.demo.enums.ReferenceType;
import com.pharmacy.demo.exception.BadRequestException;
import com.pharmacy.demo.exception.ConflictException;
import com.pharmacy.demo.exception.NotFoundException;
import com.pharmacy.demo.exception.UnprocessableException;
import com.pharmacy.demo.repository.CustomerRepository;
import com.pharmacy.demo.repository.InvoiceRepository;
import com.pharmacy.demo.repository.MedicineRepository;
import com.pharmacy.demo.security.Actor;
import com.pharmacy.demo.service.ActivityLogger;
import com.pharmacy.demo.service.CheckoutService;
import com.pharmacy.demo.service.CodeGenerator;
import com.pharmacy.demo.service.CodeKind;
import com.pharmacy.demo.service.FefoAllocator;
import com.pharmacy.demo.service.IdGenerator;
import com.pharmacy.demo.service.InvoiceService;
import com.pharmacy.demo.service.LoyaltyService;
import com.pharmacy.demo.service.StockChange;
import com.pharmacy.demo.service.StockLedgerService;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class CheckoutServiceImpl implements CheckoutService {

    private static final int MAX_ATTEMPTS = 5;
    private static final int WRITE_CONFLICT_CODE = 112;
    private static final String TRANSIENT_LABEL = "TransientTransactionError";
    private static final String WALK_IN_CUSTOMER = "Khách lẻ";

    private final InvoiceRepository invoiceRepository;
    private final MedicineRepository medicineRepository;
    private final CustomerRepository customerRepository;
    private final FefoAllocator fefoAllocator;
    private final LoyaltyService loyaltyService;
    private final StockLedgerService stockLedgerService;
    private final CodeGenerator codeGenerator;
    private final IdGenerator idGenerator;
    private final ActivityLogger activityLogger;
    private final InvoiceService invoiceService;
    private final TransactionTemplate transactionTemplate;
    private final MongoTemplate mongoTemplate;
    private final Clock clock;

    @Override
    public CodePreviewResponse nextNumber() {
        return new CodePreviewResponse(codeGenerator.preview(CodeKind.INVOICE));
    }

    @Override
    public InvoiceDetailResponse create(CreateInvoiceRequest request, Actor actor) {
        String requestId = blankToNull(request.requestId());
        for (int attempt = 1; ; attempt++) {
            Invoice existing = existingInvoice(requestId, actor);
            if (existing != null) {
                return invoiceService.get(existing.getId(), actor);
            }
            try {
                Invoice created = transactionTemplate.execute(status -> place(request, requestId, actor));
                return invoiceService.get(created.getId(), actor);
            } catch (RuntimeException exception) {
                if (attempt >= MAX_ATTEMPTS || !retryable(exception, requestId)) {
                    throw exception;
                }
            }
        }
    }

    private Invoice existingInvoice(String requestId, Actor actor) {
        if (requestId == null) {
            return null;
        }
        Invoice existing = invoiceRepository.findByRequestId(requestId).orElse(null);
        if (existing != null && !actor.id().equals(existing.getEmployeeId())) {
            throw new ConflictException("Mã yêu cầu đã được sử dụng.");
        }
        return existing;
    }

    private boolean retryable(RuntimeException exception, String requestId) {
        if (requestId != null && exception instanceof DuplicateKeyException) {
            return true;
        }
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof MongoException mongo
                    && (mongo.hasErrorLabel(TRANSIENT_LABEL) || mongo.getCode() == WRITE_CONFLICT_CODE)) {
                return true;
            }
        }
        return false;
    }

    private Invoice place(CreateInvoiceRequest request, String requestId, Actor actor) {
        Customer customer = resolveCustomer(request.customerId());
        List<InvoiceLine> lines = buildLines(request.items());
        checkPrescription(lines, request);
        long subtotal = lines.stream().mapToLong(InvoiceLine::getSubtotal).sum();
        DiscountType discountType = request.discountType() == null ? DiscountType.AMOUNT : request.discountType();
        long discountValue = request.discountValue() == null ? 0 : request.discountValue();
        long discountAmount = discountAmount(discountType, discountValue, subtotal);
        long payable = subtotal - discountAmount;
        int pointsUsed = request.pointsUsed() == null ? 0 : request.pointsUsed();
        long pointsDiscount = loyaltyService.redeemValue(customer, pointsUsed, payable);
        long total = payable - pointsDiscount;
        int pointsEarned = loyaltyService.earnedPoints(customer, total);
        long received = receivedAmount(request, total);
        for (InvoiceLine line : lines) {
            Medicine medicine =
                    medicineRepository.findById(line.getMedicineId()).orElseThrow();
            line.setAllocations(fefoAllocator.allocate(medicine, line.getQuantity()));
        }
        String invoiceId = idGenerator.newId();
        String code = codeGenerator.next(CodeKind.INVOICE);
        deductStock(lines, invoiceId, code, actor);
        boolean restricted = isRestricted(lines);
        Invoice invoice = invoiceRepository.insert(Invoice.builder()
                .id(invoiceId)
                .code(code)
                .employeeId(actor.id())
                .employeeName(actor.name())
                .customerId(customer == null ? null : customer.getId())
                .customerName(customer == null ? WALK_IN_CUSTOMER : customer.getName())
                .items(lines)
                .subtotal(subtotal)
                .discountType(discountType)
                .discountValue(discountValue)
                .discountAmount(discountAmount)
                .pointsUsed(pointsUsed)
                .pointsDiscount(pointsDiscount)
                .pointsEarned(pointsEarned)
                .total(total)
                .paymentMethod(request.paymentMethod())
                .receivedAmount(received)
                .changeAmount(received - total)
                .prescriptionConfirmed(restricted && Boolean.TRUE.equals(request.prescriptionConfirmed()))
                .prescriptionCode(restricted ? blankToNull(request.prescriptionCode()) : null)
                .status(InvoiceStatus.COMPLETED)
                .requestId(requestId)
                .note(blankToNull(request.note()))
                .createdAt(clock.instant())
                .build());
        loyaltyService.settle(invoice, actor);
        removeDraft(request.draftId(), actor);
        activityLogger.log(
                actor,
                "CREATE_INVOICE",
                "INVOICE",
                invoice.getId(),
                invoice.getCode(),
                "Lập hóa đơn " + invoice.getCode() + " trị giá " + total + " đồng.");
        return invoice;
    }

    private Customer resolveCustomer(String customerId) {
        if (blankToNull(customerId) == null) {
            return null;
        }
        return customerRepository
                .findById(customerId)
                .orElseThrow(() -> new BadRequestException(
                        "Khách hàng không tồn tại.", Map.of("customerId", "Khách hàng không tồn tại.")));
    }

    private List<InvoiceLine> buildLines(List<InvoiceItemRequest> items) {
        Set<String> seen = new HashSet<>();
        List<InvoiceLine> lines = new ArrayList<>();
        for (InvoiceItemRequest item : items) {
            if (!seen.add(item.medicineId())) {
                throw new BadRequestException(
                        "Mỗi loại thuốc chỉ được xuất hiện một lần trong hóa đơn.",
                        Map.of("items", "Thuốc bị trùng trong hóa đơn."));
            }
            Medicine medicine = sellableMedicine(item.medicineId());
            lines.add(InvoiceLine.builder()
                    .medicineId(medicine.getId())
                    .medicineCode(medicine.getCode())
                    .medicineName(medicine.getName())
                    .unit(medicine.getUnit())
                    .prescriptionType(medicine.getPrescriptionType())
                    .quantity(item.quantity())
                    .unitPrice(medicine.getPrice())
                    .subtotal(medicine.getPrice() * item.quantity())
                    .build());
        }
        return lines;
    }

    private Medicine sellableMedicine(String medicineId) {
        Medicine medicine = medicineRepository
                .findById(medicineId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy thuốc."));
        if (medicine.getStatus() != MedicineStatus.ACTIVE) {
            throw new ConflictException("Thuốc " + medicine.getName() + " đã ngừng kinh doanh.");
        }
        if (medicine.getPrescriptionType() == PrescriptionType.UNVERIFIED) {
            throw new ConflictException("Thuốc " + medicine.getName() + " chưa được xác minh nên chưa thể bán.");
        }
        if (medicine.getPrice() == null || medicine.getPrice() <= 0) {
            throw new ConflictException("Thuốc " + medicine.getName() + " chưa có giá bán.");
        }
        return medicine;
    }

    private boolean isRestricted(List<InvoiceLine> lines) {
        return lines.stream()
                .anyMatch(line -> line.getPrescriptionType() == PrescriptionType.RX
                        || line.getPrescriptionType() == PrescriptionType.CONTROLLED);
    }

    private void checkPrescription(List<InvoiceLine> lines, CreateInvoiceRequest request) {
        if (!isRestricted(lines)) {
            return;
        }
        Map<String, String> errors = new LinkedHashMap<>();
        if (!Boolean.TRUE.equals(request.prescriptionConfirmed())) {
            errors.put("prescriptionConfirmed", "Cần xác nhận đã kiểm tra đơn thuốc.");
        }
        boolean controlled = lines.stream().anyMatch(line -> line.getPrescriptionType() == PrescriptionType.CONTROLLED);
        if (controlled && blankToNull(request.prescriptionCode()) == null) {
            errors.put("prescriptionCode", "Thuốc kiểm soát đặc biệt cần mã đơn thuốc.");
        }
        if (controlled && blankToNull(request.customerId()) == null) {
            errors.put("customerId", "Thuốc kiểm soát đặc biệt cần thông tin khách hàng.");
        }
        if (!errors.isEmpty()) {
            throw new UnprocessableException(
                    "PRESCRIPTION_REQUIRED", "Hóa đơn có thuốc kê đơn cần xác nhận đơn thuốc hợp lệ.", errors);
        }
    }

    private long discountAmount(DiscountType type, long value, long subtotal) {
        if (type == DiscountType.PERCENT) {
            if (value > 100) {
                throw new BadRequestException(
                        "Giảm giá theo phần trăm tối đa 100.", Map.of("discountValue", "Tối đa 100%."));
            }
            return (subtotal * value + 50) / 100;
        }
        if (value > subtotal) {
            throw new BadRequestException(
                    "Giảm giá không được vượt quá tổng tiền hàng.",
                    Map.of("discountValue", "Không được vượt quá tổng tiền hàng."));
        }
        return value;
    }

    private long receivedAmount(CreateInvoiceRequest request, long total) {
        if (request.paymentMethod() != PaymentMethod.CASH) {
            return total;
        }
        long received = request.receivedAmount() == null ? 0 : request.receivedAmount();
        if (received < total) {
            throw new BadRequestException(
                    "Tiền khách đưa chưa đủ để thanh toán.",
                    Map.of("receivedAmount", "Tiền khách đưa chưa đủ để thanh toán."));
        }
        return received;
    }

    private void deductStock(List<InvoiceLine> lines, String invoiceId, String code, Actor actor) {
        for (InvoiceLine line : lines) {
            for (BatchAllocation allocation : line.getAllocations()) {
                stockLedgerService.apply(StockChange.builder()
                        .batchId(allocation.getBatchId())
                        .delta(-allocation.getQuantity())
                        .type(MovementType.OUT)
                        .referenceType(ReferenceType.INVOICE)
                        .referenceId(invoiceId)
                        .referenceCode(code)
                        .actor(actor)
                        .note("Bán hàng theo hóa đơn " + code)
                        .allowedStatuses(Set.of(BatchHandlingStatus.AVAILABLE))
                        .build());
            }
        }
    }

    private void removeDraft(String draftId, Actor actor) {
        if (blankToNull(draftId) == null) {
            return;
        }
        mongoTemplate.remove(
                Query.query(Criteria.where("_id").is(draftId).and("employeeId").is(actor.id())), InvoiceDraft.class);
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
