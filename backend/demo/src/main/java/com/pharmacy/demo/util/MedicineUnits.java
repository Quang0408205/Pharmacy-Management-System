package com.pharmacy.demo.util;

import java.util.List;

public final class MedicineUnits {

    public static final List<String> ALLOWED =
            List.of("Hộp", "Vỉ", "Viên", "Chai", "Lọ", "Tuýp", "Gói", "Ống", "Cái", "Bộ", "Cuộn", "Hũ", "Túi");

    private MedicineUnits() {}

    public static boolean isAllowed(String unit) {
        return unit != null && ALLOWED.contains(unit.trim());
    }
}
