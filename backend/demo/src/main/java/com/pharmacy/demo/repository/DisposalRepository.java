package com.pharmacy.demo.repository;

import com.pharmacy.demo.entity.Disposal;
import com.pharmacy.demo.enums.DisposalStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface DisposalRepository extends MongoRepository<Disposal, String> {

    long countByStatus(DisposalStatus status);
}
