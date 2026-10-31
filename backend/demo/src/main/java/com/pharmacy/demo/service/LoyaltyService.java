package com.pharmacy.demo.service;

import com.pharmacy.demo.dto.response.LoyaltyEntryResponse;
import com.pharmacy.demo.entity.Customer;
import com.pharmacy.demo.entity.Invoice;
import com.pharmacy.demo.entity.embedded.LoyaltySetting;
import com.pharmacy.demo.security.Actor;
import java.util.List;

public interface LoyaltyService {

    LoyaltySetting settings();

    long redeemValue(Customer customer, int points, long payable);

    int earnedPoints(Customer customer, long paid);

    void settle(Invoice invoice, Actor actor);

    void reverse(Invoice invoice, Actor actor);

    List<LoyaltyEntryResponse> history(String customerId);
}
