package com.pharmacy.demo.dto.response;

import com.pharmacy.demo.enums.MembershipTier;
import java.util.List;

public record LoyaltySummaryResponse(boolean enabled, long totalMembers, List<TierSummary> tiers) {

    public record TierSummary(MembershipTier tier, long minSpent, int earnRatePercent, long customerCount) {}
}
