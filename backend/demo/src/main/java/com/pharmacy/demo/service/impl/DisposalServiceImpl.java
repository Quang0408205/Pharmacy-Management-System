package com.pharmacy.demo.service.impl;

import com.pharmacy.demo.dto.common.ListPager;
import com.pharmacy.demo.dto.common.PageQuery;
import com.pharmacy.demo.dto.common.PageResponse;
import com.pharmacy.demo.dto.response.DisposalDetailResponse;
import com.pharmacy.demo.dto.response.DisposalSummaryResponse;
import com.pharmacy.demo.entity.Disposal;
import com.pharmacy.demo.entity.embedded.DisposalLine;
import com.pharmacy.demo.enums.BatchHandlingStatus;
import com.pharmacy.demo.enums.DisposalStatus;
import com.pharmacy.demo.exception.ConflictException;
import com.pharmacy.demo.exception.NotFoundException;
import com.pharmacy.demo.mapper.DisposalMapper;
import com.pharmacy.demo.repository.DisposalRepository;
import com.pharmacy.demo.security.Actor;
import com.pharmacy.demo.service.ActivityLogger;
import com.pharmacy.demo.service.DisposalService;
import com.pharmacy.demo.service.StockLedgerService;
import com.pharmacy.demo.util.TextSearch;
import java.time.Clock;
import java.util.Comparator;
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
public class DisposalServiceImpl implements DisposalService {

    private static final String NOT_PENDING_MESSAGE = "Phiếu xuất hủy không ở trạng thái chờ duyệt.";
    private static final Map<String, Comparator<DisposalSummaryResponse>> SORTABLE = Map.of(
            "code", ListPager.text(DisposalSummaryResponse::code),
            "createdAt", ListPager.comparable(DisposalSummaryResponse::createdAt),
            "status", ListPager.comparable(DisposalSummaryResponse::status),
            "createdByName", ListPager.text(DisposalSummaryResponse::createdByName),
            "totalQuantity", ListPager.comparable(DisposalSummaryResponse::totalQuantity));

    private final DisposalRepository disposalRepository;
    private final StockLedgerService stockLedgerService;
    private final DisposalMapper disposalMapper;
    private final ActivityLogger activityLogger;
    private final MongoTemplate mongoTemplate;
    private final Clock clock;

    @Override
    public PageResponse<DisposalSummaryResponse> search(String q, DisposalStatus status, PageQuery query) {
        List<DisposalSummaryResponse> items = disposalRepository.findAll().stream()
                .filter(disposal -> status == null || disposal.getStatus() == status)
                .filter(disposal ->
                        TextSearch.matchesAny(q, disposal.getCode(), disposal.getCreatedByName(), disposal.getReason()))
                .map(disposalMapper::toSummary)
                .toList();
        return ListPager.paginate(items, query, SORTABLE, "createdAt,desc");
    }

    @Override
    public DisposalDetailResponse get(String id) {
        return disposalMapper.toDetail(find(id));
    }

    @Override
    @Transactional
    public DisposalDetailResponse approve(String id, String note, Actor actor) {
        Disposal disposal =
                claim(id, DisposalStatus.APPROVED, note == null || note.isBlank() ? "Đã duyệt" : note.trim(), actor);
        activityLogger.log(
                actor,
                "APPROVE_DISPOSAL",
                "DISPOSAL",
                disposal.getId(),
                disposal.getCode(),
                "Duyệt phiếu xuất hủy " + disposal.getCode());
        return disposalMapper.toDetail(disposal);
    }

    @Override
    @Transactional
    public DisposalDetailResponse reject(String id, String note, Actor actor) {
        Disposal disposal = claim(id, DisposalStatus.REJECTED, note.trim(), actor);
        for (DisposalLine line : disposal.getLines()) {
            stockLedgerService.changeHandlingStatus(
                    line.getBatchId(), BatchHandlingStatus.AVAILABLE, Set.of(BatchHandlingStatus.QUARANTINED));
        }
        activityLogger.log(
                actor,
                "REJECT_DISPOSAL",
                "DISPOSAL",
                disposal.getId(),
                disposal.getCode(),
                "Từ chối phiếu xuất hủy " + disposal.getCode());
        return disposalMapper.toDetail(disposal);
    }

    private Disposal find(String id) {
        return disposalRepository
                .findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy phiếu xuất hủy."));
    }

    private Disposal claim(String id, DisposalStatus target, String reviewNote, Actor actor) {
        find(id);
        Disposal claimed = mongoTemplate.findAndModify(
                Query.query(Criteria.where("_id").is(id).and("status").is(DisposalStatus.PENDING_APPROVAL)),
                new Update()
                        .set("status", target)
                        .set("reviewedById", actor.id())
                        .set("reviewedByName", actor.name())
                        .set("reviewedAt", clock.instant())
                        .set("reviewNote", reviewNote),
                FindAndModifyOptions.options().returnNew(true),
                Disposal.class);
        if (claimed == null) {
            throw new ConflictException(NOT_PENDING_MESSAGE);
        }
        return claimed;
    }
}
