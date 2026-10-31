package com.pharmacy.demo.mapper;

import com.pharmacy.demo.dto.response.InvoiceDraftResponse;
import com.pharmacy.demo.entity.InvoiceDraft;
import org.mapstruct.Mapper;

@Mapper
public interface InvoiceDraftMapper {

    InvoiceDraftResponse toResponse(InvoiceDraft draft);
}
