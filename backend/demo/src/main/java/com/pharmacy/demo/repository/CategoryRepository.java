package com.pharmacy.demo.repository;

import com.pharmacy.demo.entity.Category;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CategoryRepository extends MongoRepository<Category, String> {

    boolean existsByCodeIgnoreCase(String code);

    Optional<Category> findByCodeIgnoreCase(String code);
}
