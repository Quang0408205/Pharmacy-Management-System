package com.pharmacy.demo.service;

import com.pharmacy.demo.enums.MembershipTier;

public record TierStatus(MembershipTier tier, MembershipTier nextTier, long amountToNextTier, int earnRatePercent) {}
