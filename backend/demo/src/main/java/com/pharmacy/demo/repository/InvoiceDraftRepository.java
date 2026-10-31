package com.pharmacy.demo.repository;

import com.pharmacy.demo.entity.InvoiceDraft;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface InvoiceDraftRepository extends MongoRepository<InvoiceDraft, String> {

    List<InvoiceDraft> findByEmployeeIdOrderBySavedAtDesc(String employeeId);

    Optional<InvoiceDraft> findByIdAndEmployeeId(String id, String employeeId);

    long countByEmployeeId(String employeeId);

    long deleteByIdAndEmployeeId(String id, String employeeId);
}
