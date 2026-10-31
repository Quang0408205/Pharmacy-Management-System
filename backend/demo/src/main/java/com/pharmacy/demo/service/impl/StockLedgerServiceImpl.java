package com.pharmacy.demo.service.impl;

import com.pharmacy.demo.entity.Medicine;
import com.pharmacy.demo.entity.StockBatch;
import com.pharmacy.demo.entity.StockMovement;
import com.pharmacy.demo.enums.BatchHandlingStatus;
import com.pharmacy.demo.enums.MovementType;
import com.pharmacy.demo.enums.ReferenceType;
import com.pharmacy.demo.exception.ConflictException;
import com.pharmacy.demo.exception.NotFoundException;
import com.pharmacy.demo.repository.MedicineRepository;
import com.pharmacy.demo.repository.StockBatchRepository;
import com.pharmacy.demo.repository.StockMovementRepository;
import com.pharmacy.demo.security.Actor;
import com.pharmacy.demo.service.IdGenerator;
import com.pharmacy.demo.service.StockChange;
import com.pharmacy.demo.service.StockLedgerService;
import java.time.Clock;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StockLedgerServiceImpl implements StockLedgerService {

    private final MongoTemplate mongoTemplate;
    private final StockBatchRepository batchRepository;
    private final StockMovementRepository movementRepository;
    private final MedicineRepository medicineRepository;
    private final IdGenerator idGenerator;
    private final Clock clock;

    @Override
    public StockBatch receiveBatch(
            StockBatch batch,
            ReferenceType referenceType,
            String referenceId,
            String referenceCode,
            Actor actor,
            String note) {
        batch.setRemainingQty(batch.getInitialQty());
        batch.setHandlingStatus(BatchHandlingStatus.AVAILABLE);
        batch.setLedgerVersion(0L);
        batch.setCreatedAt(clock.instant());
        batch.setUpdatedAt(batch.getCreatedAt());
        StockBatch saved = batchRepository.insert(batch);
        recordMovement(
                saved, MovementType.IN, saved.getInitialQty(), referenceType, referenceId, referenceCode, actor, note);
        return saved;
    }

    @Override
    public StockBatch apply(StockChange change) {
        Criteria criteria = Criteria.where("_id").is(change.batchId());
        if (change.delta() < 0) {
            criteria = criteria.and("remainingQty").gte(-change.delta());
        }
        if (change.expectedVersion() != null) {
            criteria = criteria.and("ledgerVersion").is(change.expectedVersion());
        }
        if (change.allowedStatuses() != null) {
            criteria = criteria.and("handlingStatus").in(change.allowedStatuses());
        }
        StockBatch updated = mongoTemplate.findAndModify(
                Query.query(criteria),
                new Update()
                        .inc("remainingQty", change.delta())
                        .inc("ledgerVersion", 1)
                        .set("updatedAt", clock.instant()),
                FindAndModifyOptions.options().returnNew(true),
                StockBatch.class);
        if (updated == null) {
            throw rejection(change);
        }
        recordMovement(
                updated,
                change.type(),
                change.delta(),
                change.referenceType(),
                change.referenceId(),
                change.referenceCode(),
                change.actor(),
                change.note());
        return updated;
    }

    @Override
    public StockBatch changeHandlingStatus(
            String batchId, BatchHandlingStatus target, Set<BatchHandlingStatus> allowedFrom) {
        Criteria criteria = Criteria.where("_id").is(batchId);
        if (allowedFrom != null) {
            criteria = criteria.and("handlingStatus").in(allowedFrom);
        }
        StockBatch updated = mongoTemplate.findAndModify(
                Query.query(criteria),
                new Update()
                        .set("handlingStatus", target)
                        .inc("ledgerVersion", 1)
                        .set("updatedAt", clock.instant()),
                FindAndModifyOptions.options().returnNew(true),
                StockBatch.class);
        if (updated == null) {
            StockBatch current = batchRepository.findById(batchId).orElseThrow(this::batchNotFound);
            throw new ConflictException(
                    "Lô " + current.getCode() + " không ở trạng thái phù hợp để thực hiện thao tác.");
        }
        return updated;
    }

    private void recordMovement(
            StockBatch batch,
            MovementType type,
            int delta,
            ReferenceType referenceType,
            String referenceId,
            String referenceCode,
            Actor actor,
            String note) {
        Medicine medicine = medicineRepository.findById(batch.getMedicineId()).orElse(null);
        movementRepository.insert(StockMovement.builder()
                .id(idGenerator.newId())
                .medicineId(batch.getMedicineId())
                .medicineCode(medicine == null ? null : medicine.getCode())
                .medicineName(medicine == null ? null : medicine.getName())
                .batchId(batch.getId())
                .batchCode(batch.getCode())
                .type(type)
                .quantity(delta)
                .balanceAfter(batch.getRemainingQty())
                .referenceType(referenceType)
                .referenceId(referenceId)
                .referenceCode(referenceCode)
                .performedById(actor.id())
                .performedByName(actor.name())
                .createdAt(clock.instant())
                .note(note)
                .build());
    }

    private RuntimeException rejection(StockChange change) {
        StockBatch current = batchRepository.findById(change.batchId()).orElse(null);
        if (current == null) {
            return batchNotFound();
        }
        if (change.expectedVersion() != null && current.getLedgerVersion() != change.expectedVersion()) {
            return new ConflictException(
                    "Lô " + current.getCode() + " đã thay đổi sau khi lập phiếu. Vui lòng kiểm tra lại.");
        }
        if (change.allowedStatuses() != null && !change.allowedStatuses().contains(current.getHandlingStatus())) {
            return new ConflictException(
                    "Lô " + current.getCode() + " không ở trạng thái cho phép thực hiện thao tác.");
        }
        return new ConflictException("Lô " + current.getCode() + " chỉ còn " + current.getRemainingQty() + ", không đủ "
                + -change.delta() + ".");
    }

    private NotFoundException batchNotFound() {
        return new NotFoundException("Không tìm thấy lô thuốc.");
    }
}
