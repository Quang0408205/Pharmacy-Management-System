package com.pharmacy.demo.repository;

import com.pharmacy.demo.entity.Customer;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CustomerRepository extends MongoRepository<Customer, String> {}
