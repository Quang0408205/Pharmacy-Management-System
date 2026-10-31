package com.pharmacy.demo.repository;

import com.pharmacy.demo.entity.StockMovement;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface StockMovementRepository extends MongoRepository<StockMovement, String> {

    List<StockMovement> findByBatchIdOrderByCreatedAtDesc(String batchId);

    List<StockMovement> findTop30ByMedicineIdOrderByCreatedAtDesc(String medicineId);
}
