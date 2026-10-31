package com.pharmacy.demo.service;

import com.pharmacy.demo.security.Actor;

public interface ActivityLogger {

    void log(Actor actor, String action, String entity, String entityId, String entityCode, String description);
}
