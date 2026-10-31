package com.pharmacy.demo.service.impl;

import com.pharmacy.demo.dto.common.ListPager;
import com.pharmacy.demo.dto.common.PageQuery;
import com.pharmacy.demo.dto.common.PageResponse;
import com.pharmacy.demo.dto.response.StocktakeDetailResponse;
import com.pharmacy.demo.dto.response.StocktakeSummaryResponse;
import com.pharmacy.demo.entity.Stocktake;
import com.pharmacy.demo.entity.embedded.StocktakeLine;
import com.pharmacy.demo.enums.MovementType;
import com.pharmacy.demo.enums.ReferenceType;
import com.pharmacy.demo.enums.StocktakeStatus;
import com.pharmacy.demo.exception.ConflictException;
import com.pharmacy.demo.exception.NotFoundException;
import com.pharmacy.demo.mapper.StocktakeMapper;
import com.pharmacy.demo.repository.StocktakeRepository;
import com.pharmacy.demo.security.Actor;
import com.pharmacy.demo.service.ActivityLogger;
import com.pharmacy.demo.service.StockChange;
import com.pharmacy.demo.service.StockLedgerService;
import com.pharmacy.demo.service.StocktakeService;
import com.pharmacy.demo.util.TextSearch;
import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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
public class StocktakeServiceImpl implements StocktakeService {

    private static final String NOT_PENDING_MESSAGE = "Phiếu kiểm kê không ở trạng thái chờ duyệt.";
    private static final String DEFAULT_ADJUSTMENT_NOTE = "Điều chỉnh sau kiểm kê";
    private static final Map<String, Comparator<StocktakeSummaryResponse>> SORTABLE = Map.of(
            "code", ListPager.text(StocktakeSummaryResponse::code),
            "createdAt", ListPager.comparable(StocktakeSummaryResponse::createdAt),
            "scopeLabel", ListPager.text(StocktakeSummaryResponse::scopeLabel),
            "status", ListPager.comparable(StocktakeSummaryResponse::status),
            "createdByName", ListPager.text(StocktakeSummaryResponse::createdByName));

    private final StocktakeRepository stocktakeRepository;
    private final StockLedgerService stockLedgerService;
    private final StocktakeMapper stocktakeMapper;
    private final ActivityLogger activityLogger;
    private final MongoTemplate mongoTemplate;
    private final Clock clock;

    @Override
    public PageResponse<StocktakeSummaryResponse> search(String q, StocktakeStatus status, PageQuery query) {
        List<StocktakeSummaryResponse> items = stocktakeRepository.findAll().stream()
                .filter(stocktake -> status == null || stocktake.getStatus() == status)
                .filter(stocktake -> TextSearch.matchesAny(
                        q, stocktake.getCode(), stocktake.getScopeLabel(), stocktake.getCreatedByName()))
                .map(stocktakeMapper::toSummary)
                .toList();
        return ListPager.paginate(items, query, SORTABLE, "createdAt,desc");
    }

    @Override
    public StocktakeDetailResponse get(String id) {
        return stocktakeMapper.toDetail(find(id));
    }

    @Override
    @Transactional
    public StocktakeDetailResponse approve(String id, String note, Actor actor) {
        Stocktake stocktake =
                claim(id, StocktakeStatus.APPROVED, note == null || note.isBlank() ? "Đã duyệt" : note, actor);
        for (StocktakeLine line : stocktake.getLines()) {
            int difference = line.getActualQty() == null ? 0 : line.getActualQty() - line.getSystemQty();
            if (difference == 0) {
                continue;
            }
            stockLedgerService.apply(new StockChange(
                    line.getBatchId(),
                    difference,
                    MovementType.ADJUSTMENT,
                    ReferenceType.STOCKTAKE,
                    stocktake.getId(),
                    stocktake.getCode(),
                    actor,
                    line.getReason() == null || line.getReason().isBlank() ? DEFAULT_ADJUSTMENT_NOTE : line.getReason(),
                    line.getBatchVersion(),
                    null));
        }
        activityLogger.log(
                actor,
                "APPROVE_STOCKTAKE",
                "STOCKTAKE",
                stocktake.getId(),
                stocktake.getCode(),
                "Duyệt phiếu kiểm kê " + stocktake.getCode());
        return stocktakeMapper.toDetail(stocktake);
    }

    @Override
    @Transactional
    public StocktakeDetailResponse reject(String id, String note, Actor actor) {
        Stocktake stocktake = claim(id, StocktakeStatus.REJECTED, note.trim(), actor);
        activityLogger.log(
                actor,
                "REJECT_STOCKTAKE",
                "STOCKTAKE",
                stocktake.getId(),
                stocktake.getCode(),
                "Từ chối phiếu kiểm kê " + stocktake.getCode());
        return stocktakeMapper.toDetail(stocktake);
    }

    private Stocktake find(String id) {
        return stocktakeRepository
                .findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy phiếu kiểm kê."));
    }

    private Stocktake claim(String id, StocktakeStatus target, String reviewNote, Actor actor) {
        find(id);
        Stocktake claimed = mongoTemplate.findAndModify(
                Query.query(Criteria.where("_id").is(id).and("status").is(StocktakeStatus.PENDING_APPROVAL)),
                new Update()
                        .set("status", target)
                        .set("reviewedById", actor.id())
                        .set("reviewedByName", actor.name())
                        .set("reviewedAt", clock.instant())
                        .set("reviewNote", reviewNote),
                FindAndModifyOptions.options().returnNew(true),
                Stocktake.class);
        if (claimed == null) {
            throw new ConflictException(NOT_PENDING_MESSAGE);
        }
        return claimed;
    }
}
