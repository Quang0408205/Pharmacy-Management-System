package com.pharmacy.demo.service.impl;

import com.pharmacy.demo.dto.common.PageQuery;
import com.pharmacy.demo.dto.common.PageResponse;
import com.pharmacy.demo.dto.response.ActivityResponse;
import com.pharmacy.demo.entity.ActivityLog;
import com.pharmacy.demo.mapper.ActivityLogMapper;
import com.pharmacy.demo.service.ActivityLogService;
import com.pharmacy.demo.util.TextSearch;
import java.time.Clock;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ActivityLogServiceImpl implements ActivityLogService {

    private static final Set<String> SORTABLE = Set.of("createdAt", "actorName", "role", "action");
    private static final String DEFAULT_SORT = "createdAt,desc";

    private final MongoTemplate mongoTemplate;
    private final ActivityLogMapper activityLogMapper;
    private final Clock clock;

    @Override
    public PageResponse<ActivityResponse> search(ActivityFilter filter, PageQuery query) {
        Pageable pageable = query.toPageable(SORTABLE, DEFAULT_SORT);
        Query base = Query.query(criteriaOf(filter));
        if (filter.q() == null || filter.q().isBlank()) {
            long total = mongoTemplate.count(Query.of(base).limit(0).skip(0), ActivityLog.class);
            List<ActivityResponse> items = mongoTemplate.find(Query.of(base).with(pageable), ActivityLog.class).stream()
                    .map(activityLogMapper::toResponse)
                    .toList();
            return page(items, query, total);
        }
        List<ActivityResponse> matching =
                mongoTemplate.find(Query.of(base).with(pageable.getSort()), ActivityLog.class).stream()
                        .filter(log -> TextSearch.matchesAny(
                                filter.q(),
                                log.getActorName(),
                                log.getDescription(),
                                log.getEntityCode(),
                                log.getAction()))
                        .map(activityLogMapper::toResponse)
                        .toList();
        int from = (int) Math.min(pageable.getOffset(), matching.size());
        int to = Math.min(from + query.size(), matching.size());
        return page(matching.subList(from, to), query, matching.size());
    }

    private PageResponse<ActivityResponse> page(List<ActivityResponse> items, PageQuery query, long total) {
        int totalPages = Math.max(1, (int) Math.ceil(total / (double) query.size()));
        return new PageResponse<>(items, query.page(), query.size(), total, totalPages);
    }

    private Criteria criteriaOf(ActivityFilter filter) {
        Criteria criteria = new Criteria();
        if (filter.role() != null) {
            criteria = criteria.and("role").is(filter.role());
        }
        if (filter.from() != null || filter.to() != null) {
            Criteria range = Criteria.where("createdAt");
            if (filter.from() != null) {
                range = range.gte(filter.from().atStartOfDay(clock.getZone()).toInstant());
            }
            if (filter.to() != null) {
                range = range.lt(
                        filter.to().plusDays(1).atStartOfDay(clock.getZone()).toInstant());
            }
            criteria = new Criteria().andOperator(criteria, range);
        }
        return criteria;
    }
}
