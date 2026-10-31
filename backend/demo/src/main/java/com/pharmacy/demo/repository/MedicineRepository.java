package com.pharmacy.demo.repository;

import com.pharmacy.demo.entity.Medicine;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MedicineRepository extends MongoRepository<Medicine, String> {

    List<Medicine> findByCategoryId(String categoryId);

    boolean existsByCategoryId(String categoryId);

    long countByCategoryId(String categoryId);

    Optional<Medicine> findByCodeIgnoreCase(String code);

    Optional<Medicine> findByBarcode(String barcode);
}
