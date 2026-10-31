package com.pharmacy.demo.repository;

import com.pharmacy.demo.entity.StockBatch;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface StockBatchRepository extends MongoRepository<StockBatch, String> {

    List<StockBatch> findByMedicineId(String medicineId);

    boolean existsByMedicineIdAndCode(String medicineId, String code);
}
