package com.pharmacy.demo.service;

import com.pharmacy.demo.dto.request.SettingsRequest;
import com.pharmacy.demo.entity.SystemSetting;
import com.pharmacy.demo.security.Actor;

public interface SystemSettingService {

    SystemSetting current();

    SystemSetting update(SettingsRequest request, Actor actor);
}
