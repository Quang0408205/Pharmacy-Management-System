package com.pharmacy.demo.service.impl;

import com.pharmacy.demo.dto.request.SettingsRequest;
import com.pharmacy.demo.entity.SystemSetting;
import com.pharmacy.demo.entity.embedded.AppearanceSetting;
import com.pharmacy.demo.entity.embedded.LoyaltySetting;
import com.pharmacy.demo.entity.embedded.NotificationSetting;
import com.pharmacy.demo.entity.embedded.PharmacyInfo;
import com.pharmacy.demo.exception.BadRequestException;
import com.pharmacy.demo.repository.SystemSettingRepository;
import com.pharmacy.demo.security.Actor;
import com.pharmacy.demo.service.ActivityLogger;
import com.pharmacy.demo.service.SystemSettingService;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SystemSettingServiceImpl implements SystemSettingService {

    private static final int TIER_COUNT = 3;
    private static final int MAX_TIER_DAYS = 365;
    private static final String TIERS_MESSAGE = "Ba ngưỡng phải là số nguyên tăng dần trong khoảng 1-365 ngày.";

    private final SystemSettingRepository repository;
    private final ActivityLogger activityLogger;
    private final Clock clock;

    @Override
    public SystemSetting current() {
        SystemSetting setting =
                repository.findById(SystemSetting.DEFAULT_ID).orElseGet(() -> repository.save(defaults()));
        if (setting.getLoyalty() == null) {
            setting.setLoyalty(LoyaltySetting.defaults());
            repository.save(setting);
        }
        return setting;
    }

    @Override
    @Transactional
    public SystemSetting update(SettingsRequest request, Actor actor) {
        requireValidTiers(request.expiryTiers());
        SystemSetting setting = current();
        mergePharmacy(setting.getPharmacy(), request.pharmacy());
        mergeAppearance(setting.getAppearance(), request.appearance());
        mergeNotifications(setting.getNotifications(), request.notifications());
        mergeLoyalty(setting.getLoyalty(), request.loyalty());
        if (request.lowStockMultiplier() != null) {
            setting.setLowStockMultiplier(request.lowStockMultiplier());
        }
        if (request.expiryWarningDays() != null) {
            setting.setExpiryWarningDays(request.expiryWarningDays());
        }
        if (request.expiryTiers() != null) {
            setting.setExpiryTiers(List.copyOf(request.expiryTiers()));
        }
        setting.setUpdatedById(actor.id());
        setting.setUpdatedAt(clock.instant());
        repository.save(setting);
        activityLogger.log(
                actor,
                "UPDATE_SETTINGS",
                "SETTINGS",
                SystemSetting.DEFAULT_ID,
                "SETTINGS",
                "Cập nhật cài đặt hệ thống");
        return setting;
    }

    private static void requireValidTiers(List<Integer> tiers) {
        if (tiers == null) {
            return;
        }
        boolean valid = tiers.size() == TIER_COUNT
                && tiers.stream().allMatch(value -> value != null && value >= 1 && value <= MAX_TIER_DAYS);
        for (int index = 1; valid && index < tiers.size(); index++) {
            valid = tiers.get(index - 1) < tiers.get(index);
        }
        if (!valid) {
            throw new BadRequestException("Cài đặt chưa hợp lệ.", Map.of("expiryTiers", TIERS_MESSAGE));
        }
    }

    private static void mergePharmacy(PharmacyInfo target, SettingsRequest.Pharmacy source) {
        if (source == null) {
            return;
        }
        if (source.name() != null) {
            target.setName(source.name().trim());
        }
        if (source.address() != null) {
            target.setAddress(source.address().trim());
        }
        if (source.phone() != null) {
            target.setPhone(source.phone().trim());
        }
        if (source.email() != null) {
            target.setEmail(source.email().trim());
        }
        if (source.taxCode() != null) {
            target.setTaxCode(source.taxCode().trim());
        }
        if (source.license() != null) {
            target.setLicense(source.license().trim());
        }
    }

    private static void mergeAppearance(AppearanceSetting target, SettingsRequest.Appearance source) {
        if (source == null) {
            return;
        }
        if (source.theme() != null) {
            target.setTheme(source.theme());
        }
        if (source.density() != null) {
            target.setDensity(source.density());
        }
    }

    private static void mergeNotifications(NotificationSetting target, SettingsRequest.Notifications source) {
        if (source == null) {
            return;
        }
        if (source.lowStock() != null) {
            target.setLowStock(source.lowStock());
        }
        if (source.expiring() != null) {
            target.setExpiring(source.expiring());
        }
        if (source.newReceipt() != null) {
            target.setNewReceipt(source.newReceipt());
        }
        if (source.dailyReport() != null) {
            target.setDailyReport(source.dailyReport());
        }
    }

    private static void mergeLoyalty(LoyaltySetting target, SettingsRequest.Loyalty source) {
        if (source == null) {
            return;
        }
        if (source.enabled() != null) {
            target.setEnabled(source.enabled());
        }
        if (source.earnAmountPerPoint() != null) {
            target.setEarnAmountPerPoint(source.earnAmountPerPoint());
        }
        if (source.pointValue() != null) {
            target.setPointValue(source.pointValue());
        }
        if (source.maxRedeemPercent() != null) {
            target.setMaxRedeemPercent(source.maxRedeemPercent());
        }
    }

    private SystemSetting defaults() {
        return SystemSetting.builder()
                .id(SystemSetting.DEFAULT_ID)
                .pharmacy(PharmacyInfo.builder()
                        .name("Nhà thuốc Minh Tâm")
                        .address("125 Nguyễn Trãi, Quận 1, TP. Hồ Chí Minh")
                        .phone("028 3822 1234")
                        .email("lienhe@minhtam.vn")
                        .taxCode("0312345678")
                        .license("GPP-2024-0457")
                        .build())
                .expiryWarningDays(90)
                .expiryTiers(List.of(30, 60, 90))
                .lowStockMultiplier(1.0)
                .appearance(AppearanceSetting.builder()
                        .theme("light")
                        .density("comfortable")
                        .build())
                .notifications(NotificationSetting.builder()
                        .lowStock(true)
                        .expiring(true)
                        .newReceipt(true)
                        .dailyReport(false)
                        .build())
                .loyalty(LoyaltySetting.defaults())
                .currency("VND")
                .timezone("Asia/Ho_Chi_Minh")
                .updatedAt(clock.instant())
                .build();
    }
}
