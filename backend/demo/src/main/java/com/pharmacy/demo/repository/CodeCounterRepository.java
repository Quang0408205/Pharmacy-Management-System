package com.pharmacy.demo.repository;

import com.pharmacy.demo.entity.CodeCounter;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CodeCounterRepository extends MongoRepository<CodeCounter, String> {}
