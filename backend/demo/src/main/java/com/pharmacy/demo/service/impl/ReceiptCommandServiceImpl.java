package com.pharmacy.demo.service.impl;

import com.pharmacy.demo.dto.request.ReceiptItemRequest;
import com.pharmacy.demo.dto.request.ReceiptRequest;
import com.pharmacy.demo.dto.response.ReceiptDetailResponse;
import com.pharmacy.demo.entity.GoodsReceipt;
import com.pharmacy.demo.entity.Medicine;
import com.pharmacy.demo.entity.StockBatch;
import com.pharmacy.demo.entity.Supplier;
import com.pharmacy.demo.entity.embedded.ReceiptLine;
import com.pharmacy.demo.enums.ReceiptStatus;
import com.pharmacy.demo.enums.ReferenceType;
import com.pharmacy.demo.enums.SupplierStatus;
import com.pharmacy.demo.exception.BadRequestException;
import com.pharmacy.demo.exception.ConflictException;
import com.pharmacy.demo.exception.NotFoundException;
import com.pharmacy.demo.mapper.ReceiptMapper;
import com.pharmacy.demo.repository.GoodsReceiptRepository;
import com.pharmacy.demo.repository.MedicineRepository;
import com.pharmacy.demo.repository.StockBatchRepository;
import com.pharmacy.demo.repository.SupplierRepository;
import com.pharmacy.demo.security.Actor;
import com.pharmacy.demo.service.ActivityLogger;
import com.pharmacy.demo.service.CodeGenerator;
import com.pharmacy.demo.service.CodeKind;
import com.pharmacy.demo.service.IdGenerator;
import com.pharmacy.demo.service.ReceiptCommandService;
import com.pharmacy.demo.service.StockLedgerService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReceiptCommandServiceImpl implements ReceiptCommandService {

    private static final String NOT_FOUND_MESSAGE = "Không tìm thấy phiếu nhập.";
    private static final String INVALID_LINES_MESSAGE =
            "Phiếu nhập có dòng chưa hợp lệ. Vui lòng kiểm tra các dòng được đánh dấu.";

    private final GoodsReceiptRepository receiptRepository;
    private final SupplierRepository supplierRepository;
    private final MedicineRepository medicineRepository;
    private final StockBatchRepository batchRepository;
    private final StockLedgerService stockLedgerService;
    private final CodeGenerator codeGenerator;
    private final IdGenerator idGenerator;
    private final ReceiptMapper receiptMapper;
    private final ActivityLogger activityLogger;
    private final MongoTemplate mongoTemplate;
    private final Clock clock;

    @Override
    public String nextCode() {
        return codeGenerator.preview(CodeKind.RECEIPT);
    }

    @Override
    @Transactional
    public ReceiptDetailResponse create(ReceiptRequest request, Actor actor) {
        GoodsReceipt receipt = GoodsReceipt.builder()
                .id(idGenerator.newId())
                .code(codeGenerator.next(CodeKind.RECEIPT))
                .createdById(actor.id())
                .createdByName(actor.name())
                .status(ReceiptStatus.DRAFT)
                .createdAt(clock.instant())
                .build();
        return persist(receipt, request, actor);
    }

    @Override
    @Transactional
    public ReceiptDetailResponse update(String id, ReceiptRequest request, Actor actor) {
        receiptRepository.findById(id).orElseThrow(() -> new NotFoundException(NOT_FOUND_MESSAGE));
        GoodsReceipt receipt = mongoTemplate.findAndModify(
                Query.query(Criteria.where("_id").is(id).and("status").is(ReceiptStatus.DRAFT)),
                new Update().set("status", ReceiptStatus.DRAFT),
                FindAndModifyOptions.options().returnNew(true),
                GoodsReceipt.class);
        if (receipt == null) {
            throw new ConflictException("Phiếu nhập đã xác nhận, không thể chỉnh sửa.");
        }
        return persist(receipt, request, actor);
    }

    @Override
    @Transactional
    public void delete(String id, Actor actor) {
        GoodsReceipt receipt =
                receiptRepository.findById(id).orElseThrow(() -> new NotFoundException(NOT_FOUND_MESSAGE));
        long removed = mongoTemplate
                .remove(
                        Query.query(Criteria.where("_id").is(id).and("status").is(ReceiptStatus.DRAFT)),
                        GoodsReceipt.class)
                .getDeletedCount();
        if (removed == 0) {
            throw new ConflictException("Chỉ được xóa phiếu nháp.");
        }
        activityLogger.log(
                actor,
                "DELETE_RECEIPT_DRAFT",
                "RECEIPT",
                receipt.getId(),
                receipt.getCode(),
                "Xóa phiếu nháp " + receipt.getCode());
    }

    private ReceiptDetailResponse persist(GoodsReceipt receipt, ReceiptRequest request, Actor actor) {
        boolean confirm = Boolean.TRUE.equals(request.confirm());
        Supplier supplier = supplierOf(request.supplierId(), confirm);
        LocalDate receivingDate = request.receivingDate() == null ? LocalDate.now(clock) : request.receivingDate();
        receipt.setSupplierId(supplier.getId());
        receipt.setSupplierName(supplier.getName());
        receipt.setReceivingDate(receivingDate);
        receipt.setNote(blankToNull(request.note()));
        List<ReceiptLine> lines = buildLines(request.items(), receivingDate, confirm);
        receipt.setItems(lines);
        receipt.setTotalQuantity(
                lines.stream().mapToInt(ReceiptLine::getQuantity).sum());
        receipt.setTotalAmount(
                lines.stream().mapToLong(ReceiptLine::getSubtotal).sum());
        if (confirm) {
            receipt.setStatus(ReceiptStatus.CONFIRMED);
            receipt.setConfirmedAt(clock.instant());
            receiveBatches(receipt, actor);
        }
        GoodsReceipt saved = receiptRepository.save(receipt);
        activityLogger.log(
                actor,
                confirm ? "CONFIRM_RECEIPT" : "SAVE_RECEIPT_DRAFT",
                "RECEIPT",
                saved.getId(),
                saved.getCode(),
                (confirm ? "Xác nhận phiếu nhập " : "Lưu nháp phiếu nhập ") + saved.getCode() + " ("
                        + saved.getTotalQuantity() + " đơn vị)");
        return receiptMapper.toDetail(saved);
    }

    private Supplier supplierOf(String supplierId, boolean confirm) {
        Supplier supplier = supplierRepository
                .findById(supplierId)
                .orElseThrow(() -> new BadRequestException(
                        "Nhà cung cấp không tồn tại.", Map.of("supplierId", "Nhà cung cấp không tồn tại.")));
        if (confirm && supplier.getStatus() != SupplierStatus.ACTIVE) {
            throw new BadRequestException(
                    "Nhà cung cấp đã ngừng hợp tác.", Map.of("supplierId", "Nhà cung cấp đã ngừng hợp tác."));
        }
        return supplier;
    }

    private List<ReceiptLine> buildLines(List<ReceiptItemRequest> items, LocalDate receivingDate, boolean strict) {
        Map<String, String> errors = new LinkedHashMap<>();
        Set<String> seenBatches = new HashSet<>();
        List<ReceiptLine> lines = new ArrayList<>();
        LocalDate today = LocalDate.now(clock);
        for (int index = 0; index < items.size(); index++) {
            ReceiptItemRequest item = items.get(index);
            Medicine medicine = item.medicineId() == null
                    ? null
                    : medicineRepository.findById(item.medicineId()).orElse(null);
            if (medicine == null) {
                throw new BadRequestException("Dòng " + (index + 1) + ": thuốc không tồn tại.");
            }
            String prefix = "items." + index + ".";
            if (item.quantity() == null || item.quantity() <= 0) {
                errors.put(prefix + "quantity", "Số lượng phải lớn hơn 0.");
            }
            if (item.purchasePrice() == null || item.purchasePrice() < 0) {
                errors.put(prefix + "purchasePrice", "Đơn giá nhập không hợp lệ.");
            }
            String batchCode = item.batchCode() == null || item.batchCode().isBlank()
                    ? null
                    : item.batchCode().trim().toUpperCase(Locale.ROOT);
            if (strict) {
                checkBatchFields(errors, prefix, item, batchCode, medicine, receivingDate, today);
                if (batchCode != null && !seenBatches.add(medicine.getId() + "|" + batchCode)) {
                    errors.put(prefix + "batchCode", "Số lô bị trùng trong phiếu.");
                }
            }
            int quantity = item.quantity() == null ? 0 : item.quantity();
            long price = item.purchasePrice() == null ? 0 : item.purchasePrice();
            lines.add(ReceiptLine.builder()
                    .lineNo(index + 1)
                    .medicineId(medicine.getId())
                    .medicineCode(medicine.getCode())
                    .medicineName(medicine.getName())
                    .batchCode(batchCode)
                    .manufactureDate(item.manufactureDate())
                    .expiryDate(item.expiryDate())
                    .quantity(quantity)
                    .unit(medicine.getUnit())
                    .purchasePrice(price)
                    .subtotal(quantity * price)
                    .build());
        }
        if (!errors.isEmpty()) {
            throw new BadRequestException(INVALID_LINES_MESSAGE, errors);
        }
        return lines;
    }

    private void checkBatchFields(
            Map<String, String> errors,
            String prefix,
            ReceiptItemRequest item,
            String batchCode,
            Medicine medicine,
            LocalDate receivingDate,
            LocalDate today) {
        if (batchCode == null) {
            errors.put(prefix + "batchCode", "Số lô là bắt buộc.");
        } else if (batchRepository.existsByMedicineIdAndCode(medicine.getId(), batchCode)) {
            errors.put(prefix + "batchCode", "Số lô đã tồn tại cho thuốc này.");
        }
        if (item.expiryDate() == null) {
            errors.put(prefix + "expiryDate", "Hạn sử dụng là bắt buộc.");
            return;
        }
        if (!item.expiryDate().isAfter(receivingDate) || !item.expiryDate().isAfter(today)) {
            errors.put(prefix + "expiryDate", "Hạn sử dụng phải sau ngày nhập và sau hôm nay.");
        }
        if (item.manufactureDate() != null
                && (!item.manufactureDate().isBefore(item.expiryDate())
                        || item.manufactureDate().isAfter(receivingDate))) {
            errors.put(prefix + "manufactureDate", "Ngày sản xuất phải trước hạn dùng và không sau ngày nhập.");
        }
    }

    private void receiveBatches(GoodsReceipt receipt, Actor actor) {
        for (ReceiptLine line : receipt.getItems()) {
            StockBatch batch = stockLedgerService.receiveBatch(
                    StockBatch.builder()
                            .id(idGenerator.newId())
                            .code(line.getBatchCode())
                            .medicineId(line.getMedicineId())
                            .receiptId(receipt.getId())
                            .receiptLineNo(line.getLineNo())
                            .supplierId(receipt.getSupplierId())
                            .manufactureDate(line.getManufactureDate())
                            .expiryDate(line.getExpiryDate())
                            .receivedDate(receipt.getReceivingDate())
                            .initialQty(line.getQuantity())
                            .purchasePrice(line.getPurchasePrice())
                            .build(),
                    ReferenceType.RECEIPT,
                    receipt.getId(),
                    receipt.getCode(),
                    actor,
                    "Nhập từ " + receipt.getSupplierName());
            line.setBatchId(batch.getId());
            if (line.getPurchasePrice() > 0) {
                mongoTemplate.updateFirst(
                        Query.query(Criteria.where("_id").is(line.getMedicineId())),
                        new Update().set("costPrice", line.getPurchasePrice()).set("updatedAt", clock.instant()),
                        Medicine.class);
            }
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
