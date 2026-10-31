package com.pharmacy.demo.mapper;

import com.pharmacy.demo.dto.response.SettingsResponse;
import com.pharmacy.demo.entity.SystemSetting;
import com.pharmacy.demo.entity.embedded.AppearanceSetting;
import com.pharmacy.demo.entity.embedded.LoyaltySetting;
import com.pharmacy.demo.entity.embedded.NotificationSetting;
import com.pharmacy.demo.entity.embedded.PharmacyInfo;
import org.mapstruct.Mapper;

@Mapper
public interface SettingMapper {

    SettingsResponse toResponse(SystemSetting setting);

    SettingsResponse.Pharmacy toPharmacy(PharmacyInfo info);

    SettingsResponse.Appearance toAppearance(AppearanceSetting appearance);

    SettingsResponse.Notifications toNotifications(NotificationSetting notifications);

    SettingsResponse.Loyalty toLoyalty(LoyaltySetting loyalty);
}
