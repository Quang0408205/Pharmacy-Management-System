package com.pharmacy.demo.mapper;

import com.pharmacy.demo.dto.response.InvoiceDetailResponse;
import com.pharmacy.demo.dto.response.InvoiceSummaryResponse;
import com.pharmacy.demo.entity.Invoice;
import com.pharmacy.demo.entity.embedded.InvoiceLine;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(imports = InvoiceLine.class)
public interface InvoiceMapper {

    @Mapping(target = "itemCount", expression = "java(invoice.getItems() == null ? 0 : invoice.getItems().size())")
    @Mapping(
            target = "quantityTotal",
            expression =
                    "java(invoice.getItems() == null ? 0 : invoice.getItems().stream().mapToInt(InvoiceLine::getQuantity).sum())")
    InvoiceSummaryResponse toSummary(Invoice invoice);

    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "employee", ignore = true)
    @Mapping(target = "pharmacy", ignore = true)
    InvoiceDetailResponse toDetail(Invoice invoice);
}
