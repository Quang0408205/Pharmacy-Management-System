package com.pharmacy.demo.repository;

import com.pharmacy.demo.entity.SystemSetting;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SystemSettingRepository extends MongoRepository<SystemSetting, String> {}
