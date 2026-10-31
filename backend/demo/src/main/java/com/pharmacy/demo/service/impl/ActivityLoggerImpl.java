package com.pharmacy.demo.service.impl;

import com.pharmacy.demo.entity.ActivityLog;
import com.pharmacy.demo.repository.ActivityLogRepository;
import com.pharmacy.demo.security.Actor;
import com.pharmacy.demo.service.ActivityLogger;
import com.pharmacy.demo.service.IdGenerator;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ActivityLoggerImpl implements ActivityLogger {

    private final ActivityLogRepository repository;
    private final IdGenerator idGenerator;
    private final Clock clock;

    @Override
    public void log(Actor actor, String action, String entity, String entityId, String entityCode, String description) {
        repository.save(ActivityLog.builder()
                .id(idGenerator.newId())
                .actorId(actor.id())
                .actorName(actor.name())
                .role(actor.role())
                .action(action)
                .entity(entity)
                .entityId(entityId)
                .entityCode(entityCode)
                .description(description)
                .createdAt(clock.instant())
                .build());
    }
}
