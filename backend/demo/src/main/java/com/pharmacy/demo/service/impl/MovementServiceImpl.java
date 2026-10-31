package com.pharmacy.demo.service.impl;

import com.pharmacy.demo.dto.common.ListPager;
import com.pharmacy.demo.dto.common.PageQuery;
import com.pharmacy.demo.dto.common.PageResponse;
import com.pharmacy.demo.dto.response.MovementResponse;
import com.pharmacy.demo.entity.StockMovement;
import com.pharmacy.demo.mapper.MovementMapper;
import com.pharmacy.demo.service.MovementService;
import com.pharmacy.demo.util.TextSearch;
import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MovementServiceImpl implements MovementService {

    private static final Map<String, Comparator<MovementResponse>> SORTABLE = Map.of(
            "createdAt", ListPager.comparable(MovementResponse::createdAt),
            "medicineName", ListPager.text(MovementResponse::medicineName),
            "batchCode", ListPager.text(MovementResponse::batchCode),
            "quantity", ListPager.comparable(MovementResponse::quantity),
            "type", ListPager.comparable(MovementResponse::type),
            "referenceCode", ListPager.text(MovementResponse::referenceCode),
            "performedByName", ListPager.text(MovementResponse::performedByName));

    private final MongoTemplate mongoTemplate;
    private final MovementMapper movementMapper;
    private final Clock clock;

    @Override
    public PageResponse<MovementResponse> search(MovementFilter filter, PageQuery query) {
        List<MovementResponse> items = mongoTemplate.find(Query.query(criteriaOf(filter)), StockMovement.class).stream()
                .filter(movement -> TextSearch.matchesAny(
                        filter.q(),
                        movement.getMedicineName(),
                        movement.getBatchCode(),
                        movement.getReferenceCode(),
                        movement.getPerformedByName()))
                .map(movementMapper::toResponse)
                .toList();
        return ListPager.paginate(items, query, SORTABLE, "createdAt,desc");
    }

    private Criteria criteriaOf(MovementFilter filter) {
        Criteria criteria = new Criteria();
        if (filter.type() != null) {
            criteria = criteria.and("type").is(filter.type());
        }
        if (filter.medicineId() != null) {
            criteria = criteria.and("medicineId").is(filter.medicineId());
        }
        if (filter.batchId() != null) {
            criteria = criteria.and("batchId").is(filter.batchId());
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
