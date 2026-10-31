package com.pharmacy.demo.repository;

import com.pharmacy.demo.entity.Invoice;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface InvoiceRepository extends MongoRepository<Invoice, String> {

    Optional<Invoice> findByRequestId(String requestId);

    List<Invoice> findTop30ByCustomerIdOrderByCreatedAtDesc(String customerId);
}
