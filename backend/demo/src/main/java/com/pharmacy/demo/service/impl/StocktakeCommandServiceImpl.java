package com.pharmacy.demo.service.impl;

import com.pharmacy.demo.dto.request.StocktakeCreateRequest;
import com.pharmacy.demo.dto.request.StocktakeLineRequest;
import com.pharmacy.demo.dto.request.StocktakeSaveRequest;
import com.pharmacy.demo.dto.response.StocktakeDetailResponse;
import com.pharmacy.demo.entity.Category;
import com.pharmacy.demo.entity.Medicine;
import com.pharmacy.demo.entity.StockBatch;
import com.pharmacy.demo.entity.Stocktake;
import com.pharmacy.demo.entity.embedded.StocktakeLine;
import com.pharmacy.demo.enums.StocktakeStatus;
import com.pharmacy.demo.exception.BadRequestException;
import com.pharmacy.demo.exception.ConflictException;
import com.pharmacy.demo.exception.NotFoundException;
import com.pharmacy.demo.mapper.StocktakeMapper;
import com.pharmacy.demo.repository.CategoryRepository;
import com.pharmacy.demo.repository.MedicineRepository;
import com.pharmacy.demo.repository.StocktakeRepository;
import com.pharmacy.demo.security.Actor;
import com.pharmacy.demo.service.ActivityLogger;
import com.pharmacy.demo.service.CodeGenerator;
import com.pharmacy.demo.service.CodeKind;
import com.pharmacy.demo.service.IdGenerator;
import com.pharmacy.demo.service.StocktakeCommandService;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StocktakeCommandServiceImpl implements StocktakeCommandService {

    private static final String ALL_SCOPE = "ALL";
    private static final String ALL_SCOPE_LABEL = "Toàn bộ kho";
    private static final String NOT_FOUND_MESSAGE = "Không tìm thấy phiếu kiểm kê.";
    private static final String NOT_DRAFT_MESSAGE = "Phiếu kiểm kê không còn ở trạng thái nháp.";
    private static final int MAX_LINES = 60;

    private final StocktakeRepository stocktakeRepository;
    private final CategoryRepository categoryRepository;
    private final MedicineRepository medicineRepository;
    private final StocktakeMapper stocktakeMapper;
    private final CodeGenerator codeGenerator;
    private final IdGenerator idGenerator;
    private final ActivityLogger activityLogger;
    private final MongoTemplate mongoTemplate;
    private final Clock clock;

    @Override
    @Transactional
    public StocktakeDetailResponse create(StocktakeCreateRequest request, Actor actor) {
        boolean wholeWarehouse = request.categoryId() == null
                || request.categoryId().isBlank()
                || ALL_SCOPE.equals(request.categoryId());
        Category category = wholeWarehouse
                ? null
                : categoryRepository
                        .findById(request.categoryId())
                        .orElseThrow(() -> new BadRequestException(
                                "Danh mục không tồn tại.", Map.of("categoryId", "Danh mục không tồn tại.")));
        List<StocktakeLine> lines = snapshotLines(category);
        if (lines.isEmpty()) {
            throw new ConflictException("Không có lô nào còn tồn trong phạm vi đã chọn.");
        }
        Stocktake stocktake = stocktakeRepository.insert(Stocktake.builder()
                .id(idGenerator.newId())
                .code(codeGenerator.next(CodeKind.STOCKTAKE))
                .categoryId(category == null ? null : category.getId())
                .scopeLabel(category == null ? ALL_SCOPE_LABEL : category.getName())
                .note(blankToNull(request.note()))
                .createdById(actor.id())
                .createdByName(actor.name())
                .lines(lines)
                .status(StocktakeStatus.DRAFT)
                .createdAt(clock.instant())
                .build());
        activityLogger.log(
                actor,
                "CREATE_STOCKTAKE",
                "STOCKTAKE",
                stocktake.getId(),
                stocktake.getCode(),
                "Tạo phiếu kiểm kê " + stocktake.getCode() + " (" + stocktake.getScopeLabel() + ")");
        return stocktakeMapper.toDetail(stocktake);
    }

    @Override
    @Transactional
    public StocktakeDetailResponse save(String id, StocktakeSaveRequest request, Actor actor) {
        Stocktake stocktake = find(id);
        requireDraft(stocktake);
        List<StocktakeLine> lines = mergeLines(stocktake.getLines(), request);
        Update update = new Update().set("lines", lines);
        if (request != null && request.note() != null) {
            update.set("note", blankToNull(request.note()));
        }
        writeIfDraft(id, update);
        return stocktakeMapper.toDetail(find(id));
    }

    @Override
    @Transactional
    public StocktakeDetailResponse submit(String id, StocktakeSaveRequest request, Actor actor) {
        Stocktake stocktake = find(id);
        requireDraft(stocktake);
        List<StocktakeLine> lines = mergeLines(stocktake.getLines(), request);
        requireComplete(lines);
        Update update = new Update()
                .set("lines", lines)
                .set("status", StocktakeStatus.PENDING_APPROVAL)
                .set("submittedAt", clock.instant());
        if (request != null && request.note() != null) {
            update.set("note", blankToNull(request.note()));
        }
        writeIfDraft(id, update);
        Stocktake submitted = find(id);
        activityLogger.log(
                actor,
                "SUBMIT_STOCKTAKE",
                "STOCKTAKE",
                submitted.getId(),
                submitted.getCode(),
                "Gửi duyệt phiếu kiểm kê " + submitted.getCode());
        return stocktakeMapper.toDetail(submitted);
    }

    @Override
    @Transactional
    public void delete(String id, Actor actor) {
        Stocktake stocktake = find(id);
        long removed = mongoTemplate
                .remove(
                        Query.query(Criteria.where("_id").is(id).and("status").is(StocktakeStatus.DRAFT)),
                        Stocktake.class)
                .getDeletedCount();
        if (removed == 0) {
            throw new ConflictException("Chỉ được xóa phiếu kiểm kê nháp.");
        }
        activityLogger.log(
                actor,
                "DELETE_STOCKTAKE",
                "STOCKTAKE",
                stocktake.getId(),
                stocktake.getCode(),
                "Xóa phiếu kiểm kê nháp " + stocktake.getCode());
    }

    private Stocktake find(String id) {
        return stocktakeRepository.findById(id).orElseThrow(() -> new NotFoundException(NOT_FOUND_MESSAGE));
    }

    private void requireDraft(Stocktake stocktake) {
        if (stocktake.getStatus() != StocktakeStatus.DRAFT) {
            throw new ConflictException(NOT_DRAFT_MESSAGE);
        }
    }

    private void writeIfDraft(String id, Update update) {
        long matched = mongoTemplate
                .updateFirst(
                        Query.query(Criteria.where("_id").is(id).and("status").is(StocktakeStatus.DRAFT)),
                        update,
                        Stocktake.class)
                .getMatchedCount();
        if (matched == 0) {
            throw new ConflictException(NOT_DRAFT_MESSAGE);
        }
    }

    private List<StocktakeLine> snapshotLines(Category category) {
        Criteria criteria = Criteria.where("remainingQty").gt(0);
        if (category != null) {
            List<String> medicineIds = medicineRepository.findByCategoryId(category.getId()).stream()
                    .map(Medicine::getId)
                    .toList();
            criteria = criteria.and("medicineId").in(medicineIds);
        }
        List<StockBatch> batches = mongoTemplate.find(Query.query(criteria), StockBatch.class);
        Map<String, Medicine> medicines = new HashMap<>();
        medicineRepository
                .findAllById(batches.stream()
                        .map(StockBatch::getMedicineId)
                        .distinct()
                        .toList())
                .forEach(medicine -> medicines.put(medicine.getId(), medicine));
        return batches.stream()
                .filter(batch -> medicines.containsKey(batch.getMedicineId()))
                .sorted(Comparator.comparing((StockBatch batch) ->
                                medicines.get(batch.getMedicineId()).getName())
                        .thenComparing(StockBatch::getExpiryDate)
                        .thenComparing(StockBatch::getId))
                .limit(MAX_LINES)
                .map(batch -> StocktakeLine.builder()
                        .batchId(batch.getId())
                        .batchCode(batch.getCode())
                        .medicineId(batch.getMedicineId())
                        .medicineName(medicines.get(batch.getMedicineId()).getName())
                        .systemQty(batch.getRemainingQty())
                        .actualQty(null)
                        .difference(0)
                        .reason("")
                        .batchVersion(batch.getLedgerVersion())
                        .build())
                .toList();
    }

    private List<StocktakeLine> mergeLines(List<StocktakeLine> current, StocktakeSaveRequest request) {
        Map<String, StocktakeLineRequest> updates = new HashMap<>();
        if (request != null && request.lines() != null) {
            request.lines().forEach(line -> updates.put(line.batchId(), line));
        }
        List<StocktakeLine> merged = new ArrayList<>();
        for (StocktakeLine line : current) {
            StocktakeLineRequest update = updates.get(line.getBatchId());
            if (update == null) {
                merged.add(line);
                continue;
            }
            if (update.actualQty() != null && update.actualQty() < 0) {
                throw new BadRequestException(
                        "Số lượng thực tế không hợp lệ.",
                        Map.of("lines." + line.getBatchId(), "Số lượng thực tế không được âm."));
            }
            Integer actual = update.actualQty();
            merged.add(line.toBuilder()
                    .actualQty(actual)
                    .difference(actual == null ? 0 : actual - line.getSystemQty())
                    .reason(update.reason() == null ? "" : update.reason().trim())
                    .build());
        }
        return merged;
    }

    private void requireComplete(List<StocktakeLine> lines) {
        long missing =
                lines.stream().filter(line -> line.getActualQty() == null).count();
        if (missing > 0) {
            throw new BadRequestException("Còn " + missing + " dòng chưa nhập số lượng thực tế.");
        }
        long withoutReason = lines.stream()
                .filter(line -> line.getDifference() != 0 && line.getReason().isBlank())
                .count();
        if (withoutReason > 0) {
            throw new BadRequestException("Còn " + withoutReason + " dòng chênh lệch chưa ghi lý do.");
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
