package com.pharmacy.demo.repository;

import com.pharmacy.demo.entity.GoodsReceipt;
import com.pharmacy.demo.enums.ReceiptStatus;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface GoodsReceiptRepository extends MongoRepository<GoodsReceipt, String> {

    long countByStatus(ReceiptStatus status);

    long countByCreatedByIdAndStatus(String createdById, ReceiptStatus status);

    long countBySupplierId(String supplierId);

    List<GoodsReceipt> findTop20BySupplierIdOrderByReceivingDateDesc(String supplierId);
}
