package com.pharmacy.demo.service.impl;

import com.pharmacy.demo.dto.request.InvoiceItemRequest;
import com.pharmacy.demo.dto.request.SaveDraftRequest;
import com.pharmacy.demo.dto.response.InvoiceDraftResponse;
import com.pharmacy.demo.entity.Customer;
import com.pharmacy.demo.entity.InvoiceDraft;
import com.pharmacy.demo.entity.Medicine;
import com.pharmacy.demo.entity.embedded.DraftLine;
import com.pharmacy.demo.enums.DiscountType;
import com.pharmacy.demo.exception.BadRequestException;
import com.pharmacy.demo.exception.ConflictException;
import com.pharmacy.demo.exception.NotFoundException;
import com.pharmacy.demo.mapper.InvoiceDraftMapper;
import com.pharmacy.demo.repository.CustomerRepository;
import com.pharmacy.demo.repository.InvoiceDraftRepository;
import com.pharmacy.demo.repository.MedicineRepository;
import com.pharmacy.demo.security.Actor;
import com.pharmacy.demo.service.IdGenerator;
import com.pharmacy.demo.service.InvoiceDraftService;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InvoiceDraftServiceImpl implements InvoiceDraftService {

    private static final int MAX_DRAFTS_PER_EMPLOYEE = 20;
    private static final String NOT_FOUND_MESSAGE = "Không tìm thấy đơn nháp.";
    private static final String WALK_IN_CUSTOMER = "Khách lẻ";

    private final InvoiceDraftRepository draftRepository;
    private final MedicineRepository medicineRepository;
    private final CustomerRepository customerRepository;
    private final InvoiceDraftMapper draftMapper;
    private final IdGenerator idGenerator;
    private final Clock clock;

    @Override
    public List<InvoiceDraftResponse> list(Actor actor) {
        return draftRepository.findByEmployeeIdOrderBySavedAtDesc(actor.id()).stream()
                .map(draftMapper::toResponse)
                .toList();
    }

    @Override
    public InvoiceDraftResponse save(SaveDraftRequest request, Actor actor) {
        Instant now = clock.instant();
        InvoiceDraft draft = existingOrNew(request.id(), actor, now);
        Customer customer = resolveCustomer(request.customerId());
        draft.setCustomerId(customer == null ? null : customer.getId());
        draft.setCustomerName(customer == null ? WALK_IN_CUSTOMER : customer.getName());
        draft.setItems(buildLines(request.items()));
        draft.setDiscountType(request.discountType() == null ? DiscountType.AMOUNT : request.discountType());
        draft.setDiscountValue(request.discountValue() == null ? 0L : request.discountValue());
        draft.setPaymentMethod(request.paymentMethod());
        draft.setNote(
                request.note() == null || request.note().isBlank()
                        ? null
                        : request.note().trim());
        draft.setSavedAt(now);
        return draftMapper.toResponse(draftRepository.save(draft));
    }

    @Override
    public void delete(String id, Actor actor) {
        if (draftRepository.deleteByIdAndEmployeeId(id, actor.id()) == 0) {
            throw new NotFoundException(NOT_FOUND_MESSAGE);
        }
    }

    private InvoiceDraft existingOrNew(String id, Actor actor, Instant now) {
        if (id != null && !id.isBlank()) {
            return draftRepository
                    .findByIdAndEmployeeId(id, actor.id())
                    .orElseThrow(() -> new NotFoundException(NOT_FOUND_MESSAGE));
        }
        if (draftRepository.countByEmployeeId(actor.id()) >= MAX_DRAFTS_PER_EMPLOYEE) {
            throw new ConflictException("Bạn chỉ được lưu tối đa " + MAX_DRAFTS_PER_EMPLOYEE + " đơn nháp.");
        }
        return InvoiceDraft.builder()
                .id(idGenerator.newId())
                .employeeId(actor.id())
                .createdAt(now)
                .build();
    }

    private Customer resolveCustomer(String customerId) {
        if (customerId == null || customerId.isBlank()) {
            return null;
        }
        return customerRepository
                .findById(customerId)
                .orElseThrow(() -> new BadRequestException(
                        "Khách hàng không tồn tại.", Map.of("customerId", "Khách hàng không tồn tại.")));
    }

    private List<DraftLine> buildLines(List<InvoiceItemRequest> items) {
        Set<String> seen = new HashSet<>();
        List<DraftLine> lines = new ArrayList<>();
        for (InvoiceItemRequest item : items) {
            if (!seen.add(item.medicineId())) {
                throw new BadRequestException(
                        "Mỗi loại thuốc chỉ được xuất hiện một lần trong đơn nháp.",
                        Map.of("items", "Thuốc bị trùng trong đơn nháp."));
            }
            Medicine medicine = medicineRepository
                    .findById(item.medicineId())
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy thuốc."));
            lines.add(DraftLine.builder()
                    .medicineId(medicine.getId())
                    .medicineName(medicine.getName())
                    .unit(medicine.getUnit())
                    .quantity(item.quantity())
                    .build());
        }
        return lines;
    }
}
