package com.pharmacy.demo.service.impl;

import com.pharmacy.demo.entity.CodeCounter;
import com.pharmacy.demo.service.CodeGenerator;
import com.pharmacy.demo.service.CodeKind;
import java.time.Clock;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CodeGeneratorImpl implements CodeGenerator {

    private final MongoTemplate mongoTemplate;
    private final Clock clock;

    @Override
    public String next(CodeKind kind) {
        LocalDate today = LocalDate.now(clock);
        CodeCounter counter = mongoTemplate.findAndModify(
                byKey(kind.counterKey(today)),
                new Update().inc("value", 1).set("updatedAt", clock.instant()),
                FindAndModifyOptions.options().upsert(true).returnNew(true),
                CodeCounter.class);
        return kind.format(today, counter.getValue());
    }

    @Override
    public String preview(CodeKind kind) {
        LocalDate today = LocalDate.now(clock);
        CodeCounter counter = mongoTemplate.findOne(byKey(kind.counterKey(today)), CodeCounter.class);
        long current = counter == null ? 0 : counter.getValue();
        return kind.format(today, current + 1);
    }

    @Override
    public void raiseTo(CodeKind kind, LocalDate date, long value) {
        String key = kind.counterKey(date);
        try {
            mongoTemplate.upsert(
                    byKey(key).addCriteria(Criteria.where("value").lt(value)),
                    new Update().set("value", value).set("updatedAt", clock.instant()),
                    CodeCounter.class);
        } catch (DuplicateKeyException exception) {
            return;
        }
    }

    private Query byKey(String key) {
        return Query.query(Criteria.where("_id").is(key));
    }
}
