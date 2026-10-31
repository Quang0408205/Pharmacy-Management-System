package com.pharmacy.demo.dto.response;

import java.util.List;

public record SettingsResponse(
        Pharmacy pharmacy,
        Double lowStockMultiplier,
        Integer expiryWarningDays,
        List<Integer> expiryTiers,
        Appearance appearance,
        Notifications notifications,
        Loyalty loyalty) {

    public record Pharmacy(String name, String address, String phone, String email, String taxCode, String license) {}

    public record Appearance(String theme, String density) {}

    public record Notifications(boolean lowStock, boolean expiring, boolean newReceipt, boolean dailyReport) {}

    public record Loyalty(boolean enabled, long earnAmountPerPoint, long pointValue, int maxRedeemPercent) {}
}
