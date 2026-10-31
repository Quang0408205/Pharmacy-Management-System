package com.pharmacy.demo.dto.response;

import java.util.List;

public record ExpirySummaryResponse(List<Tier> tiers, long expired) {

    public record Tier(int days, long count) {}
}
