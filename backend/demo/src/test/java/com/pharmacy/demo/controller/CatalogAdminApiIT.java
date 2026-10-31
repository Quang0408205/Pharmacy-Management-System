package com.pharmacy.demo.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.pharmacy.demo.entity.Category;
import com.pharmacy.demo.entity.Employee;
import com.pharmacy.demo.enums.CategoryStatus;
import com.pharmacy.demo.enums.EmployeeStatus;
import com.pharmacy.demo.enums.Role;
import com.pharmacy.demo.repository.CategoryRepository;
import com.pharmacy.demo.repository.CodeCounterRepository;
import com.pharmacy.demo.repository.CustomerRepository;
import com.pharmacy.demo.repository.EmployeeRepository;
import com.pharmacy.demo.repository.MedicineRepository;
import com.pharmacy.demo.repository.SupplierRepository;
import com.pharmacy.demo.security.JwtService;
import com.pharmacy.demo.support.MongoIntegrationTest;
import jakarta.servlet.http.Cookie;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@MongoIntegrationTest
@AutoConfigureMockMvc
class CatalogAdminApiIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private MedicineRepository medicineRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CodeCounterRepository codeCounterRepository;

    @Autowired
    private JwtService jwtService;

    @BeforeEach
    void seed() {
        medicineRepository.deleteAll();
        categoryRepository.deleteAll();
        supplierRepository.deleteAll();
        customerRepository.deleteAll();
        employeeRepository.deleteAll();
        codeCounterRepository.deleteAll();
        employeeRepository.save(employee("emp-admin", "admin", Role.ADMIN));
        employeeRepository.save(employee("emp-sales", "sales", Role.SALES));
        employeeRepository.save(employee("emp-kho", "kho", Role.WAREHOUSE));
        categoryRepository.save(Category.builder()
                .id("cat-1")
                .code("KS")
                .name("Kháng sinh")
                .description("")
                .color("#0d9488")
                .status(CategoryStatus.ACTIVE)
                .createdAt(Instant.now())
                .build());
    }

    private Employee employee(String id, String username, Role role) {
        return Employee.builder()
                .id(id)
                .code("NV-" + id)
                .username(username)
                .usernameNormalized(username)
                .fullName("Người dùng " + username)
                .email(username + "@pharmacy.demo")
                .emailNormalized(username + "@pharmacy.demo")
                .passwordHash("x")
                .role(role)
                .status(EmployeeStatus.ACTIVE)
                .securityVersion(1L)
                .createdAt(Instant.now())
                .build();
    }

    private Cookie as(String id) {
        Employee employee = employeeRepository.findById(id).orElseThrow();
        return new Cookie("PMS_TOKEN", jwtService.issue(employee, Duration.ofMinutes(10)));
    }

    private ResultActions send(MockHttpServletRequestBuilder builder, String actorId, String body) throws Exception {
        MockHttpServletRequestBuilder request = builder.cookie(as(actorId));
        if (body != null) {
            request = request.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return mockMvc.perform(request);
    }

    private static String categoryBody(String code, String name) {
        return "{\"code\":\"" + code + "\",\"name\":\"" + name + "\",\"description\":\"Mô tả\"}";
    }

    private static String medicineBody(String code, String barcode, long price) {
        return "{\"code\":\"" + code + "\",\"barcode\":" + (barcode == null ? "null" : "\"" + barcode + "\"")
                + ",\"name\":\"Paracetamol 500\",\"unit\":\"Viên\",\"manufacturer\":\"Dược Hậu Giang\","
                + "\"categoryId\":\"cat-1\",\"price\":" + price + ",\"costPrice\":1000,\"prescriptionType\":\"OTC\","
                + "\"minStock\":20}";
    }

    private static String partyBody(String name, String phone) {
        return "{\"name\":\"" + name + "\",\"phone\":\"" + phone + "\",\"email\":\"\"}";
    }

    @Test
    void adminManagesCategoriesAndOthersCannot() throws Exception {
        send(post("/api/v1/categories"), "emp-admin", categoryBody("tm", "Tim mạch"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("TM"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.medicineCount").value(0));
        send(post("/api/v1/categories"), "emp-admin", categoryBody("Tm", "Khác"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.fieldErrors.code").exists());
        send(post("/api/v1/categories"), "emp-admin", categoryBody("", "")).andExpect(status().isBadRequest());
        send(post("/api/v1/categories"), "emp-sales", categoryBody("XX", "Không được"))
                .andExpect(status().isForbidden());
        send(put("/api/v1/categories/cat-1"), "emp-admin", categoryBody("KS", "Kháng sinh mới"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Kháng sinh mới"));
        send(put("/api/v1/categories/cat-1"), "emp-admin", categoryBody("TM", "Trùng mã"))
                .andExpect(status().isConflict());
        send(get("/api/v1/categories"), "emp-kho", null)
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void categoryWithMedicinesCannotBeDeleted() throws Exception {
        send(post("/api/v1/medicines"), "emp-admin", medicineBody("T0001", null, 5000))
                .andExpect(status().isCreated());
        send(delete("/api/v1/categories/cat-1"), "emp-admin", null).andExpect(status().isConflict());

        MvcResult created = send(post("/api/v1/categories"), "emp-admin", categoryBody("TRONG", "Trống"))
                .andReturn();
        String id = JsonPath.read(created.getResponse().getContentAsString(), "$.id");
        send(delete("/api/v1/categories/" + id), "emp-admin", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        assertThat(categoryRepository.findById(id)).isEmpty();
        send(delete("/api/v1/categories/" + id), "emp-admin", null).andExpect(status().isNotFound());
    }

    @Test
    void adminCreatesUpdatesAndDisablesMedicines() throws Exception {
        send(get("/api/v1/medicines/next-code"), "emp-admin", null)
                .andExpect(jsonPath("$.code").value("T0001"));

        MvcResult created = send(post("/api/v1/medicines"), "emp-admin", medicineBody("t0001", "8930001", 5000))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("T0001"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.categoryName").value("Kháng sinh"))
                .andExpect(jsonPath("$.totalQty").value(0))
                .andReturn();
        String id = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

        send(get("/api/v1/medicines/next-code"), "emp-admin", null)
                .andExpect(jsonPath("$.code").value("T0002"));
        send(post("/api/v1/medicines"), "emp-admin", medicineBody("T0001", null, 5000))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.fieldErrors.code").exists());
        send(post("/api/v1/medicines"), "emp-admin", medicineBody("T0002", "8930001", 5000))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.fieldErrors.barcode").exists());
        send(post("/api/v1/medicines"), "emp-admin", medicineBody("T0002", "", 5000))
                .andExpect(status().isCreated());
        send(post("/api/v1/medicines"), "emp-admin", medicineBody("T0003", null, 5000))
                .andExpect(status().isCreated());
        send(post("/api/v1/medicines"), "emp-admin", medicineBody("T0004", null, 0))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.price").exists());
        send(
                        post("/api/v1/medicines"),
                        "emp-admin",
                        medicineBody("T0005", null, 5000).replace("cat-1", "nope"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.categoryId").exists());

        send(put("/api/v1/medicines/" + id), "emp-admin", medicineBody("T0001", "8930001", 7000))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(7000));

        send(patch("/api/v1/medicines/" + id + "/status"), "emp-admin", "{\"status\":\"INACTIVE\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));
        send(get("/api/v1/medicines"), "emp-sales", null)
                .andExpect(jsonPath("$.total").value(2));
        send(get("/api/v1/medicines"), "emp-admin", null)
                .andExpect(jsonPath("$.total").value(3));
        send(patch("/api/v1/medicines/" + id + "/status"), "emp-admin", "{\"status\":\"BROKEN\"}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void onlyAdminsMayWriteMedicines() throws Exception {
        send(post("/api/v1/medicines"), "emp-sales", medicineBody("T0001", null, 5000))
                .andExpect(status().isForbidden());
        send(post("/api/v1/medicines"), "emp-kho", medicineBody("T0001", null, 5000))
                .andExpect(status().isForbidden());
        send(get("/api/v1/medicines/next-code"), "emp-sales", null).andExpect(status().isForbidden());
        send(put("/api/v1/medicines/x"), "emp-kho", medicineBody("T0001", null, 5000))
                .andExpect(status().isForbidden());
    }

    @Test
    void suppliersAreManagedByAdminAndWarehouseWithUniquePhones() throws Exception {
        MvcResult created = send(post("/api/v1/suppliers"), "emp-kho", partyBody("Công ty Dược A", "0901 234 567"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("NCC001"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.paymentTerms").value("Thanh toán ngay"))
                .andExpect(jsonPath("$.receiptCount").value(0))
                .andReturn();
        String id = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

        send(post("/api/v1/suppliers"), "emp-admin", partyBody("Công ty B", "0901234567"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.fieldErrors.phone").exists());
        send(post("/api/v1/suppliers"), "emp-admin", partyBody("Công ty C", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.phone").exists());
        send(put("/api/v1/suppliers/" + id), "emp-admin", partyBody("Công ty Dược A2", "0901 234 567"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Công ty Dược A2"));
        send(patch("/api/v1/suppliers/" + id + "/status"), "emp-kho", "{\"status\":\"INACTIVE\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));
        send(get("/api/v1/suppliers"), "emp-admin", null)
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].receiptCount").value(0));
        send(get("/api/v1/suppliers/" + id), "emp-kho", null)
                .andExpect(jsonPath("$.receipts.length()").value(0))
                .andExpect(jsonPath("$.totalAmount").value(0));
        send(get("/api/v1/suppliers"), "emp-sales", null).andExpect(status().isForbidden());
        send(get("/api/v1/suppliers/missing"), "emp-admin", null).andExpect(status().isNotFound());
    }

    @Test
    void customersAreManagedByAdminAndSales() throws Exception {
        MvcResult created = send(post("/api/v1/customers"), "emp-sales", partyBody("Nguyễn Văn A", "0912 345 678"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("KH0001"))
                .andExpect(jsonPath("$.gender").value("Nam"))
                .andExpect(jsonPath("$.invoiceCount").value(0))
                .andExpect(jsonPath("$.totalSpent").value(0))
                .andReturn();
        String id = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

        send(post("/api/v1/customers"), "emp-admin", partyBody("Người khác", "0912345678"))
                .andExpect(status().isConflict());
        send(put("/api/v1/customers/" + id), "emp-admin", partyBody("Nguyễn Văn B", "0912 345 678"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Nguyễn Văn B"));
        send(get("/api/v1/customers").param("q", "van b"), "emp-sales", null)
                .andExpect(jsonPath("$.total").value(1));
        send(get("/api/v1/customers/" + id), "emp-sales", null)
                .andExpect(jsonPath("$.invoices.length()").value(0))
                .andExpect(jsonPath("$.invoiceCount").value(0));
        send(get("/api/v1/customers"), "emp-kho", null).andExpect(status().isForbidden());
        send(get("/api/v1/customers/missing"), "emp-sales", null).andExpect(status().isNotFound());
    }
}
