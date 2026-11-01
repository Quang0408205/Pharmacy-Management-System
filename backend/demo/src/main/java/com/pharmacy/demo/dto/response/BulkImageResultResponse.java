package com.pharmacy.demo.dto.response;

import com.pharmacy.demo.enums.BulkImageStatus;
import java.util.List;

public record BulkImageResultResponse(int saved, int skipped, int noMatch, int invalid, List<Item> items) {

    public record Item(
            String fileName, BulkImageStatus status, String medicineCode, String medicineName, String message) {}
}
