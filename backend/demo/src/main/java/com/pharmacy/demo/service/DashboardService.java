package com.pharmacy.demo.service;

import com.pharmacy.demo.dto.response.AdminDashboardResponse;
import com.pharmacy.demo.dto.response.SalesDashboardResponse;
import com.pharmacy.demo.security.Actor;

public interface DashboardService {

    AdminDashboardResponse admin();

    SalesDashboardResponse sales(Actor actor);
}
