package com.pharmacy.demo.dto.response;

public record CategoryDistributionResponse(
        String categoryId, String name, String color, long medicines, long quantity, long value) {}
