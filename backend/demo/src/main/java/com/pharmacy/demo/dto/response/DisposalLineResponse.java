package com.pharmacy.demo.dto.response;

import com.pharmacy.demo.enums.DisposalReason;
import java.time.LocalDate;

public record DisposalLineResponse(
        String batchId,
        String batchCode,
        String medicineId,
        String medicineName,
        LocalDate expiryDate,
        DisposalReason reasonType,
        Integer quantity,
        String reasonDetail) {}
