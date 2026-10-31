package com.pharmacy.demo.repository;

import com.pharmacy.demo.entity.Stocktake;
import com.pharmacy.demo.enums.StocktakeStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface StocktakeRepository extends MongoRepository<Stocktake, String> {

    long countByStatus(StocktakeStatus status);
}
