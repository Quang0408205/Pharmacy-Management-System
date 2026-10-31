package com.pharmacy.demo.service.impl;

import com.pharmacy.demo.dto.request.DisposalLineRequest;
import com.pharmacy.demo.dto.request.DisposalRequest;
import com.pharmacy.demo.dto.response.DisposalDetailResponse;
import com.pharmacy.demo.entity.Disposal;
import com.pharmacy.demo.entity.Medicine;
import com.pharmacy.demo.entity.StockBatch;
import com.pharmacy.demo.entity.embedded.DisposalLine;
import com.pharmacy.demo.enums.BatchHandlingStatus;
import com.pharmacy.demo.enums.DisposalStatus;
import com.pharmacy.demo.enums.MovementType;
import com.pharmacy.demo.enums.ReferenceType;
import com.pharmacy.demo.exception.BadRequestException;
import com.pharmacy.demo.exception.ConflictException;
import com.pharmacy.demo.exception.NotFoundException;
import com.pharmacy.demo.mapper.DisposalMapper;
import com.pharmacy.demo.repository.DisposalRepository;
import com.pharmacy.demo.repository.MedicineRepository;
import com.pharmacy.demo.repository.StockBatchRepository;
import com.pharmacy.demo.security.Actor;
import com.pharmacy.demo.service.ActivityLogger;
import com.pharmacy.demo.service.CodeGenerator;
import com.pharmacy.demo.service.CodeKind;
import com.pharmacy.demo.service.DisposalCommandService;
import com.pharmacy.demo.service.IdGenerator;
import com.pharmacy.demo.service.StockChange;
import com.pharmacy.demo.service.StockLedgerService;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
public class DisposalCommandServiceImpl implements DisposalCommandService {

    private static final String NOT_FOUND_MESSAGE = "Không tìm thấy phiếu xuất hủy.";
    private static final String NOT_DRAFT_MESSAGE = "Phiếu xuất hủy không còn ở trạng thái nháp.";
    private static final Set<DisposalStatus> OPEN_STATUSES =
            Set.of(DisposalStatus.DRAFT, DisposalStatus.PENDING_APPROVAL, DisposalStatus.APPROVED);

    private final DisposalRepository disposalRepository;
    private final StockBatchRepository batchRepository;
    private final MedicineRepository medicineRepository;
    private final StockLedgerService stockLedgerService;
    private final DisposalMapper disposalMapper;
    private final CodeGenerator codeGenerator;
    private final IdGenerator idGenerator;
    private final ActivityLogger activityLogger;
    private final MongoTemplate mongoTemplate;
    private final Clock clock;

    @Override
    @Transactional
    public DisposalDetailResponse create(DisposalRequest request, Actor actor) {
        String id = idGenerator.newId();
        Disposal disposal = disposalRepository.insert(Disposal.builder()
                .id(id)
                .code(codeGenerator.next(CodeKind.DISPOSAL))
                .createdById(actor.id())
                .createdByName(actor.name())
                .lines(buildLines(request.lines(), id))
                .reason(request.reason().trim())
                .note(blankToNull(request.note()))
                .status(DisposalStatus.DRAFT)
                .createdAt(clock.instant())
                .build());
        activityLogger.log(
                actor,
                "CREATE_DISPOSAL",
                "DISPOSAL",
                disposal.getId(),
                disposal.getCode(),
                "Lập phiếu xuất hủy " + disposal.getCode());
        return disposalMapper.toDetail(disposal);
    }

    @Override
    @Transactional
    public DisposalDetailResponse update(String id, DisposalRequest request, Actor actor) {
        Disposal disposal = find(id);
        requireDraft(disposal);
        List<DisposalLine> lines = buildLines(request.lines(), id);
        long matched = mongoTemplate
                .updateFirst(
                        draftQuery(id),
                        new Update()
                                .set("lines", lines)
                                .set("reason", request.reason().trim())
                                .set("note", blankToNull(request.note())),
                        Disposal.class)
                .getMatchedCount();
        if (matched == 0) {
            throw new ConflictException(NOT_DRAFT_MESSAGE);
        }
        return disposalMapper.toDetail(find(id));
    }

    @Override
    @Transactional
    public void delete(String id, Actor actor) {
        Disposal disposal = find(id);
        long removed = mongoTemplate.remove(draftQuery(id), Disposal.class).getDeletedCount();
        if (removed == 0) {
            throw new ConflictException("Chỉ được xóa phiếu xuất hủy nháp.");
        }
        activityLogger.log(
                actor,
                "DELETE_DISPOSAL",
                "DISPOSAL",
                disposal.getId(),
                disposal.getCode(),
                "Xóa phiếu xuất hủy nháp " + disposal.getCode());
    }

    @Override
    @Transactional
    public DisposalDetailResponse submit(String id, Actor actor) {
        find(id);
        Disposal disposal = mongoTemplate.findAndModify(
                draftQuery(id),
                new Update().set("status", DisposalStatus.PENDING_APPROVAL).set("submittedAt", clock.instant()),
                FindAndModifyOptions.options().returnNew(true),
                Disposal.class);
        if (disposal == null) {
            throw new ConflictException(NOT_DRAFT_MESSAGE);
        }
        for (DisposalLine line : disposal.getLines()) {
            StockBatch batch = batchRepository
                    .findById(line.getBatchId())
                    .orElseThrow(() -> new ConflictException("Lô " + line.getBatchCode() + " không còn tồn tại."));
            if (batch.getRemainingQty() < line.getQuantity()) {
                throw new ConflictException("Lô " + batch.getCode() + " chỉ còn " + batch.getRemainingQty()
                        + ", không đủ " + line.getQuantity() + " để xuất hủy.");
            }
            stockLedgerService.changeHandlingStatus(
                    line.getBatchId(), BatchHandlingStatus.QUARANTINED, Set.of(BatchHandlingStatus.AVAILABLE));
        }
        activityLogger.log(
                actor,
                "SUBMIT_DISPOSAL",
                "DISPOSAL",
                disposal.getId(),
                disposal.getCode(),
                "Gửi duyệt phiếu xuất hủy " + disposal.getCode());
        return disposalMapper.toDetail(disposal);
    }

    @Override
    @Transactional
    public DisposalDetailResponse execute(String id, Actor actor) {
        find(id);
        Disposal disposal = mongoTemplate.findAndModify(
                Query.query(Criteria.where("_id").is(id).and("status").is(DisposalStatus.APPROVED)),
                new Update()
                        .set("status", DisposalStatus.EXECUTED)
                        .set("executedById", actor.id())
                        .set("executedByName", actor.name())
                        .set("executedAt", clock.instant()),
                FindAndModifyOptions.options().returnNew(true),
                Disposal.class);
        if (disposal == null) {
            throw new ConflictException("Chỉ thực hiện được phiếu xuất hủy đã được duyệt.");
        }
        for (DisposalLine line : disposal.getLines()) {
            StockBatch batch = stockLedgerService.apply(StockChange.builder()
                    .batchId(line.getBatchId())
                    .delta(-line.getQuantity())
                    .type(MovementType.WRITE_OFF)
                    .referenceType(ReferenceType.DISPOSAL)
                    .referenceId(disposal.getId())
                    .referenceCode(disposal.getCode())
                    .actor(actor)
                    .note("Xuất hủy: " + disposal.getReason())
                    .allowedStatuses(Set.of(BatchHandlingStatus.QUARANTINED))
                    .build());
            BatchHandlingStatus next =
                    batch.getRemainingQty() == 0 ? BatchHandlingStatus.DISPOSED : BatchHandlingStatus.AVAILABLE;
            stockLedgerService.changeHandlingStatus(batch.getId(), next, Set.of(BatchHandlingStatus.QUARANTINED));
        }
        activityLogger.log(
                actor,
                "EXECUTE_DISPOSAL",
                "DISPOSAL",
                disposal.getId(),
                disposal.getCode(),
                "Thực hiện xuất hủy theo phiếu " + disposal.getCode());
        return disposalMapper.toDetail(disposal);
    }

    private Disposal find(String id) {
        return disposalRepository.findById(id).orElseThrow(() -> new NotFoundException(NOT_FOUND_MESSAGE));
    }

    private void requireDraft(Disposal disposal) {
        if (disposal.getStatus() != DisposalStatus.DRAFT) {
            throw new ConflictException(NOT_DRAFT_MESSAGE);
        }
    }

    private Query draftQuery(String id) {
        return Query.query(Criteria.where("_id").is(id).and("status").is(DisposalStatus.DRAFT));
    }

    private List<DisposalLine> buildLines(List<DisposalLineRequest> requests, String disposalId) {
        Set<String> seen = new HashSet<>();
        List<DisposalLine> lines = new ArrayList<>();
        for (DisposalLineRequest request : requests) {
            if (!seen.add(request.batchId())) {
                throw new BadRequestException(
                        "Mỗi lô chỉ được xuất hiện một lần trong phiếu.", Map.of("lines", "Lô bị trùng trong phiếu."));
            }
            StockBatch batch = batchRepository
                    .findById(request.batchId())
                    .orElseThrow(() -> new BadRequestException("Lô thuốc không tồn tại."));
            if (batch.getHandlingStatus() != BatchHandlingStatus.AVAILABLE) {
                throw new ConflictException("Lô " + batch.getCode() + " đang được xử lý ở chứng từ khác.");
            }
            if (request.quantity() > batch.getRemainingQty()) {
                throw new BadRequestException(
                        "Lô " + batch.getCode() + " chỉ còn " + batch.getRemainingQty() + ", không đủ "
                                + request.quantity() + ".",
                        Map.of("lines", "Số lượng vượt quá tồn của lô " + batch.getCode() + "."));
            }
            requireNotInOpenDisposal(batch, disposalId);
            Medicine medicine =
                    medicineRepository.findById(batch.getMedicineId()).orElse(null);
            lines.add(DisposalLine.builder()
                    .batchId(batch.getId())
                    .batchCode(batch.getCode())
                    .medicineId(batch.getMedicineId())
                    .medicineName(medicine == null ? null : medicine.getName())
                    .expiryDate(batch.getExpiryDate())
                    .reasonType(request.reasonType())
                    .quantity(request.quantity())
                    .reasonDetail(blankToNull(request.reasonDetail()))
                    .build());
        }
        return lines;
    }

    private void requireNotInOpenDisposal(StockBatch batch, String disposalId) {
        boolean inOpen = mongoTemplate.exists(
                Query.query(Criteria.where("_id")
                        .ne(disposalId)
                        .and("status")
                        .in(OPEN_STATUSES)
                        .and("lines.batchId")
                        .is(batch.getId())),
                Disposal.class);
        if (inOpen) {
            throw new ConflictException("Lô " + batch.getCode() + " đã thuộc một phiếu xuất hủy khác đang xử lý.");
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
