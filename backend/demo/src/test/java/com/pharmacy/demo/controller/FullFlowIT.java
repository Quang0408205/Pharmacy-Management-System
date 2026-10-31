package com.pharmacy.demo.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.pharmacy.demo.entity.Category;
import com.pharmacy.demo.entity.Employee;
import com.pharmacy.demo.entity.Medicine;
import com.pharmacy.demo.entity.Supplier;
import com.pharmacy.demo.enums.CategoryStatus;
import com.pharmacy.demo.enums.EmployeeStatus;
import com.pharmacy.demo.enums.MedicineStatus;
import com.pharmacy.demo.enums.PrescriptionType;
import com.pharmacy.demo.enums.Role;
import com.pharmacy.demo.enums.SupplierStatus;
import com.pharmacy.demo.repository.CategoryRepository;
import com.pharmacy.demo.repository.DisposalRepository;
import com.pharmacy.demo.repository.EmployeeRepository;
import com.pharmacy.demo.repository.GoodsReceiptRepository;
import com.pharmacy.demo.repository.InvoiceDraftRepository;
import com.pharmacy.demo.repository.InvoiceRepository;
import com.pharmacy.demo.repository.MedicineRepository;
import com.pharmacy.demo.repository.StockBatchRepository;
import com.pharmacy.demo.repository.StockMovementRepository;
import com.pharmacy.demo.repository.StocktakeRepository;
import com.pharmacy.demo.repository.SupplierRepository;
import com.pharmacy.demo.security.JwtService;
import com.pharmacy.demo.support.MongoIntegrationTest;
import jakarta.servlet.http.Cookie;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@MongoIntegrationTest
@AutoConfigureMockMvc
class FullFlowIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private MedicineRepository medicineRepository;

    @Autowired
    private StockBatchRepository batchRepository;

    @Autowired
    private StockMovementRepository movementRepository;

    @Autowired
    private GoodsReceiptRepository receiptRepository;

    @Autowired
    private StocktakeRepository stocktakeRepository;

    @Autowired
    private DisposalRepository disposalRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private InvoiceDraftRepository draftRepository;

    @Autowired
    private JwtService jwtService;

    @BeforeEach
    void seed() {
        invoiceRepository.deleteAll();
        draftRepository.deleteAll();
        disposalRepository.deleteAll();
        stocktakeRepository.deleteAll();
        receiptRepository.deleteAll();
        movementRepository.deleteAll();
        batchRepository.deleteAll();
        medicineRepository.deleteAll();
        supplierRepository.deleteAll();
        categoryRepository.deleteAll();
        employeeRepository.deleteAll();
        employeeRepository.save(employee("emp-admin", "admin", Role.ADMIN));
        employeeRepository.save(employee("emp-sales", "sales", Role.SALES));
        employeeRepository.save(employee("emp-kho", "kho", Role.WAREHOUSE));
        categoryRepository.save(Category.builder()
                .id("cat-1")
                .code("DM1")
                .name("Giảm đau")
                .status(CategoryStatus.ACTIVE)
                .createdAt(Instant.now())
                .build());
        supplierRepository.save(Supplier.builder()
                .id("sup-1")
                .code("NCC001")
                .name("Dược Hậu Giang")
                .status(SupplierStatus.ACTIVE)
                .createdAt(Instant.now())
                .build());
        medicineRepository.save(Medicine.builder()
                .id("med-1")
                .code("T0001")
                .name("Paracetamol")
                .unit("Viên")
                .categoryId("cat-1")
                .price(2000L)
                .costPrice(1000L)
                .minStock(10)
                .prescriptionType(PrescriptionType.OTC)
                .status(MedicineStatus.ACTIVE)
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
                .email(username + "@nhathuoc.vn")
                .emailNormalized(username + "@nhathuoc.vn")
                .passwordHash("x")
                .role(role)
                .status(EmployeeStatus.ACTIVE)
                .securityVersion(1L)
                .createdAt(Instant.now())
                .build();
    }

    private ResultActions send(MockHttpServletRequestBuilder builder, String actorId, String body) throws Exception {
        Employee employee = employeeRepository.findById(actorId).orElseThrow();
        MockHttpServletRequestBuilder request =
                builder.cookie(new Cookie("PMS_TOKEN", jwtService.issue(employee, Duration.ofMinutes(10))));
        if (body != null) {
            request = request.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return mockMvc.perform(request);
    }

    private String field(ResultActions result, String path) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), path);
    }

    private void expectBalancedLedger() throws Exception {
        send(get("/api/v1/inventory/reconciliation"), "emp-admin", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balanced").value(true))
                .andExpect(jsonPath("$.mismatches.length()").value(0));
    }

    @Test
    void everyRoleTakesPartInOneStockLifecycleAndTheLedgerStaysBalanced() throws Exception {
        String receiptBody = "{\"supplierId\":\"sup-1\",\"receivingDate\":\"" + LocalDate.now()
                + "\",\"confirm\":true,\"items\":[{\"medicineId\":\"med-1\",\"batchCode\":\"lo-e2e\","
                + "\"expiryDate\":\"" + LocalDate.now().plusDays(300)
                + "\",\"quantity\":100,\"purchasePrice\":1200}]}";
        send(post("/api/v1/receipts"), "emp-kho", receiptBody).andExpect(status().isOk());
        String batchId = batchRepository.findAll().get(0).getId();
        expectBalancedLedger();

        String invoiceId = field(
                send(
                                post("/api/v1/invoices"),
                                "emp-sales",
                                "{\"items\":[{\"medicineId\":\"med-1\",\"quantity\":30}],\"paymentMethod\":\"CARD\","
                                        + "\"requestId\":\"e2e-1\"}")
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.total").value(60000)),
                "$.id");
        assertThat(batchRepository.findById(batchId).orElseThrow().getRemainingQty())
                .isEqualTo(70);
        expectBalancedLedger();

        send(post("/api/v1/invoices/" + invoiceId + "/cancel"), "emp-admin", "{\"reason\":\"Khách đổi ý\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        assertThat(batchRepository.findById(batchId).orElseThrow().getRemainingQty())
                .isEqualTo(100);
        expectBalancedLedger();

        String stocktakeId = field(
                send(post("/api/v1/stocktakes"), "emp-kho", "{\"categoryId\":\"ALL\"}")
                        .andExpect(status().isOk()),
                "$.id");
        send(
                        put("/api/v1/stocktakes/" + stocktakeId),
                        "emp-kho",
                        "{\"lines\":[{\"batchId\":\"" + batchId + "\",\"actualQty\":95,\"reason\":\"Vỡ vỉ\"}]}")
                .andExpect(status().isOk());
        send(post("/api/v1/stocktakes/" + stocktakeId + "/submit"), "emp-kho", "{}")
                .andExpect(status().isOk());
        send(post("/api/v1/stocktakes/" + stocktakeId + "/approve"), "emp-admin", "{}")
                .andExpect(status().isOk());
        assertThat(batchRepository.findById(batchId).orElseThrow().getRemainingQty())
                .isEqualTo(95);
        expectBalancedLedger();

        String disposalId = field(
                send(
                                post("/api/v1/disposals"),
                                "emp-kho",
                                "{\"reason\":\"Hư hỏng\",\"lines\":[{\"batchId\":\"" + batchId
                                        + "\",\"quantity\":15,\"reasonType\":\"DAMAGED\"}]}")
                        .andExpect(status().isOk()),
                "$.id");
        send(post("/api/v1/disposals/" + disposalId + "/submit"), "emp-kho", null)
                .andExpect(status().isOk());
        send(
                        post("/api/v1/invoices"),
                        "emp-sales",
                        "{\"items\":[{\"medicineId\":\"med-1\",\"quantity\":1}],\"paymentMethod\":\"CARD\"}")
                .andExpect(status().isConflict());
        send(post("/api/v1/disposals/" + disposalId + "/approve"), "emp-admin", "{}")
                .andExpect(status().isOk());
        send(post("/api/v1/disposals/" + disposalId + "/execute"), "emp-kho", null)
                .andExpect(status().isOk());
        assertThat(batchRepository.findById(batchId).orElseThrow().getRemainingQty())
                .isEqualTo(80);
        expectBalancedLedger();

        send(get("/api/v1/inventory/summary"), "emp-kho", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUnits").value(80));
    }

    @Test
    void reconciliationReportsABatchWhoseQuantityWasChangedOutsideTheLedger() throws Exception {
        String receiptBody = "{\"supplierId\":\"sup-1\",\"receivingDate\":\"" + LocalDate.now()
                + "\",\"confirm\":true,\"items\":[{\"medicineId\":\"med-1\",\"batchCode\":\"LO-X\","
                + "\"expiryDate\":\"" + LocalDate.now().plusDays(300) + "\",\"quantity\":10,\"purchasePrice\":1}]}";
        send(post("/api/v1/receipts"), "emp-kho", receiptBody).andExpect(status().isOk());
        var batch = batchRepository.findAll().get(0);
        batch.setRemainingQty(7);
        batchRepository.save(batch);

        send(get("/api/v1/inventory/reconciliation"), "emp-admin", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balanced").value(false))
                .andExpect(jsonPath("$.mismatches[0].remainingQty").value(7))
                .andExpect(jsonPath("$.mismatches[0].ledgerTotal").value(10));
        send(get("/api/v1/inventory/reconciliation"), "emp-kho", null).andExpect(status().isForbidden());
        send(get("/api/v1/inventory/reconciliation"), "emp-sales", null).andExpect(status().isForbidden());
    }
}
