package com.pharmacy.demo.bootstrap;

import com.pharmacy.demo.entity.Category;
import com.pharmacy.demo.entity.Customer;
import com.pharmacy.demo.entity.Employee;
import com.pharmacy.demo.entity.Medicine;
import com.pharmacy.demo.entity.Supplier;
import com.pharmacy.demo.entity.SystemSetting;
import java.util.List;

public record CatalogData(
        SystemSetting settings,
        List<Category> categories,
        List<Medicine> medicines,
        List<Supplier> suppliers,
        List<Customer> customers,
        List<Employee> employees) {}
