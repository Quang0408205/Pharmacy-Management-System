package com.pharmacy.demo.mapper;

import com.pharmacy.demo.dto.response.CustomerDetailResponse;
import com.pharmacy.demo.dto.response.CustomerResponse;
import com.pharmacy.demo.dto.response.InvoiceSummaryResponse;
import com.pharmacy.demo.entity.Customer;
import java.time.Instant;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper
public interface CustomerMapper {

    CustomerResponse toResponse(Customer customer, long invoiceCount, long totalSpent, Instant lastPurchase);

    CustomerDetailResponse toDetail(
            Customer customer,
            long invoiceCount,
            long totalSpent,
            Instant lastPurchase,
            List<InvoiceSummaryResponse> invoices);
}
