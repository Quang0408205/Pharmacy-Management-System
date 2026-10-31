package com.pharmacy.demo.service;

import com.pharmacy.demo.dto.response.NotificationResponse;
import com.pharmacy.demo.security.Actor;
import java.util.List;

public interface NotificationService {

    List<NotificationResponse> list(Actor actor);
}
