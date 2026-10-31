package com.pharmacy.demo.dto.response;

public record BestSellerResponse(
        String medicineId, String name, String code, String unit, long quantity, long revenue) {}
