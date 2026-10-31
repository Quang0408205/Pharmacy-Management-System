package com.pharmacy.demo.repository;

import com.pharmacy.demo.entity.ActivityLog;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ActivityLogRepository extends MongoRepository<ActivityLog, String> {

    List<ActivityLog> findTop8ByActorIdOrderByCreatedAtDesc(String actorId);
}
