package com.pharmacy.demo.repository;

import com.pharmacy.demo.entity.LoyaltyTransaction;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface LoyaltyTransactionRepository extends MongoRepository<LoyaltyTransaction, String> {

    List<LoyaltyTransaction> findTop50ByCustomerIdOrderByCreatedAtDesc(String customerId);
}
