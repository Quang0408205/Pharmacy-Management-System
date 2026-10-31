package com.pharmacy.demo.service;

import org.bson.types.ObjectId;
import org.springframework.stereotype.Component;

@Component
public class IdGenerator {

    public String newId() {
        return new ObjectId().toHexString();
    }
}
