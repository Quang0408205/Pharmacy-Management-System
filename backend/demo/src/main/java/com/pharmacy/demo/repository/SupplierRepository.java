package com.pharmacy.demo.repository;

import com.pharmacy.demo.entity.Supplier;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SupplierRepository extends MongoRepository<Supplier, String> {}
