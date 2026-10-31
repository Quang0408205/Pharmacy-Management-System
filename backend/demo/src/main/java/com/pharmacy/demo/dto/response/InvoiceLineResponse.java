package com.pharmacy.demo.dto.response;

import com.pharmacy.demo.enums.PrescriptionType;
import java.util.List;

public record InvoiceLineResponse(
        String medicineId,
        String medicineCode,
        String medicineName,
        String unit,
        PrescriptionType prescriptionType,
        Integer quantity,
        Long unitPrice,
        Long subtotal,
        List<InvoiceAllocationResponse> allocations) {}
