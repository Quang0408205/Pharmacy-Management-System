package com.pharmacy.demo.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pharmacy.demo.entity.Customer;
import com.pharmacy.demo.entity.Employee;
import com.pharmacy.demo.entity.Medicine;
import com.pharmacy.demo.entity.StockBatch;
import com.pharmacy.demo.enums.BatchHandlingStatus;
import com.pharmacy.demo.enums.EmployeeStatus;
import com.pharmacy.demo.enums.MedicineStatus;
import com.pharmacy.demo.enums.MovementType;
import com.pharmacy.demo.enums.PrescriptionType;
import com.pharmacy.demo.enums.Role;
import com.pharmacy.demo.repository.CustomerRepository;
import com.pharmacy.demo.repository.EmployeeRepository;
import com.pharmacy.demo.repository.InvoiceDraftRepository;
import com.pharmacy.demo.repository.InvoiceRepository;
import com.pharmacy.demo.repository.MedicineRepository;
import com.pharmacy.demo.repository.StockBatchRepository;
import com.pharmacy.demo.repository.StockMovementRepository;
import com.pharmacy.demo.security.JwtService;
import com.pharmacy.demo.support.MongoIntegrationTest;
import jakarta.servlet.http.Cookie;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
class CheckoutApiIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private MedicineRepository medicineRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private StockBatchRepository batchRepository;

    @Autowired
    private StockMovementRepository movementRepository;

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
        movementRepository.deleteAll();
        batchRepository.deleteAll();
        medicineRepository.deleteAll();
        customerRepository.deleteAll();
        employeeRepository.deleteAll();
        employeeRepository.save(employee("emp-admin", "admin", Role.ADMIN));
        employeeRepository.save(employee("emp-sales", "sales", Role.SALES));
        employeeRepository.save(employee("emp-sales2", "sales2", Role.SALES));
        employeeRepository.save(employee("emp-kho", "kho", Role.WAREHOUSE));
        customerRepository.save(Customer.builder()
                .id("cus-1")
                .code("KH0001")
                .name("Nguyễn Văn A")
                .phone("0901234567")
                .createdAt(Instant.now())
                .build());
        medicineRepository.save(medicine("med-otc", "T0001", "Paracetamol", 2000L, PrescriptionType.OTC));
        medicineRepository.save(medicine("med-rx", "T0002", "Amoxicillin", 5000L, PrescriptionType.RX));
        medicineRepository.save(medicine("med-ctl", "T0003", "Morphin", 9000L, PrescriptionType.CONTROLLED));
        medicineRepository.save(medicine("med-unv", "T0004", "Chưa xác minh", 1000L, PrescriptionType.UNVERIFIED));
        Medicine inactive = medicine("med-off", "T0005", "Ngừng bán", 1000L, PrescriptionType.OTC);
        inactive.setStatus(MedicineStatus.INACTIVE);
        medicineRepository.save(inactive);
        batchRepository.save(batch("b-late", "L-LATE", "med-otc", 10, 400, BatchHandlingStatus.AVAILABLE));
        batchRepository.save(batch("b-soon", "L-SOON", "med-otc", 10, 100, BatchHandlingStatus.AVAILABLE));
        batchRepository.save(batch("b-old", "L-OLD", "med-otc", 10, -5, BatchHandlingStatus.AVAILABLE));
        batchRepository.save(batch("b-quar", "L-QUAR", "med-otc", 10, 50, BatchHandlingStatus.QUARANTINED));
        batchRepository.save(batch("b-rx", "L-RX", "med-rx", 10, 200, BatchHandlingStatus.AVAILABLE));
        batchRepository.save(batch("b-ctl", "L-CTL", "med-ctl", 10, 200, BatchHandlingStatus.AVAILABLE));
        batchRepository.save(batch("b-unv", "L-UNV", "med-unv", 10, 200, BatchHandlingStatus.AVAILABLE));
        batchRepository.save(batch("b-off", "L-OFF", "med-off", 10, 200, BatchHandlingStatus.AVAILABLE));
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

    private Medicine medicine(String id, String code, String name, long price, PrescriptionType type) {
        return Medicine.builder()
                .id(id)
                .code(code)
                .name(name)
                .unit("Viên")
                .categoryId("cat-1")
                .price(price)
                .costPrice(1000L)
                .minStock(5)
                .prescriptionType(type)
                .status(MedicineStatus.ACTIVE)
                .createdAt(Instant.now())
                .build();
    }

    private StockBatch batch(
            String id, String code, String medicineId, int quantity, int expiresInDays, BatchHandlingStatus handling) {
        return StockBatch.builder()
                .id(id)
                .code(code)
                .medicineId(medicineId)
                .receiptId("rec-" + id)
                .receiptLineNo(1)
                .supplierId("sup-1")
                .receivedDate(LocalDate.now().minusDays(30))
                .expiryDate(LocalDate.now().plusDays(expiresInDays))
                .initialQty(quantity)
                .remainingQty(quantity)
                .purchasePrice(1000L)
                .handlingStatus(handling)
                .ledgerVersion(0L)
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

    private String invoiceBody(String medicineId, int quantity, String extra) {
        return "{\"items\":[{\"medicineId\":\"" + medicineId + "\",\"quantity\":" + quantity
                + "}],\"paymentMethod\":\"CASH\",\"receivedAmount\":1000000" + extra + "}";
    }

    private int remaining(String batchId) {
        return batchRepository.findById(batchId).orElseThrow().getRemainingQty();
    }

    @Test
    void checkoutAllocatesByEarliestExpiryAndComputesMoneyOnTheServer() throws Exception {
        String body = "{\"items\":[{\"medicineId\":\"med-otc\",\"quantity\":15,\"unitPrice\":1}],"
                + "\"discountType\":\"PERCENT\",\"discountValue\":10,\"paymentMethod\":\"CASH\","
                + "\"receivedAmount\":30000,\"total\":1,\"employeeId\":\"emp-admin\"}";

        send(post("/api/v1/invoices"), "emp-sales", body)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.employeeId").value("emp-sales"))
                .andExpect(jsonPath("$.customerName").value("Khách lẻ"))
                .andExpect(jsonPath("$.subtotal").value(30000))
                .andExpect(jsonPath("$.discountAmount").value(3000))
                .andExpect(jsonPath("$.total").value(27000))
                .andExpect(jsonPath("$.changeAmount").value(3000))
                .andExpect(jsonPath("$.items[0].unitPrice").value(2000))
                .andExpect(jsonPath("$.items[0].allocations[0].batchCode").value("L-SOON"))
                .andExpect(jsonPath("$.items[0].allocations[0].quantity").value(10))
                .andExpect(jsonPath("$.items[0].allocations[1].batchCode").value("L-LATE"))
                .andExpect(jsonPath("$.items[0].allocations[1].quantity").value(5));

        assertThat(remaining("b-soon")).isZero();
        assertThat(remaining("b-late")).isEqualTo(5);
        assertThat(remaining("b-old")).isEqualTo(10);
        assertThat(remaining("b-quar")).isEqualTo(10);
        assertThat(movementRepository.findAll())
                .hasSize(2)
                .allMatch(movement -> movement.getType() == MovementType.OUT);
    }

    @Test
    void sellingMoreThanSellableStockIsRejectedWithoutChangingStock() throws Exception {
        send(post("/api/v1/invoices"), "emp-sales", invoiceBody("med-otc", 21, ""))
                .andExpect(status().isConflict());

        assertThat(remaining("b-soon")).isEqualTo(10);
        assertThat(remaining("b-late")).isEqualTo(10);
        assertThat(remaining("b-old")).isEqualTo(10);
        assertThat(remaining("b-quar")).isEqualTo(10);
        assertThat(invoiceRepository.count()).isZero();
        assertThat(movementRepository.count()).isZero();
    }

    @Test
    void resubmittingTheSameRequestIdReturnsTheSameInvoiceAndDeductsOnce() throws Exception {
        String body = invoiceBody("med-otc", 3, ",\"requestId\":\"req-1\"");

        String first = send(post("/api/v1/invoices"), "emp-sales", body)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String second = send(post("/api/v1/invoices"), "emp-sales", body)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(second).isEqualTo(first);
        assertThat(invoiceRepository.count()).isEqualTo(1);
        assertThat(remaining("b-soon")).isEqualTo(7);
        assertThat(movementRepository.count()).isEqualTo(1);
    }

    @Test
    void aRequestIdOwnedByAnotherEmployeeIsRejected() throws Exception {
        String body = invoiceBody("med-otc", 1, ",\"requestId\":\"req-shared\"");
        send(post("/api/v1/invoices"), "emp-sales", body).andExpect(status().isOk());

        send(post("/api/v1/invoices"), "emp-sales2", body).andExpect(status().isConflict());
    }

    @Test
    void concurrentCheckoutsOnTheSameBatchNeverDriveStockNegative() throws Exception {
        batchRepository.deleteAll();
        batchRepository.save(batch("b-one", "L-ONE", "med-otc", 10, 100, BatchHandlingStatus.AVAILABLE));
        int threads = 4;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Integer>> results = new java.util.ArrayList<>();
        for (int index = 0; index < threads; index++) {
            String body = invoiceBody("med-otc", 4, ",\"requestId\":\"race-" + index + "\"");
            Callable<Integer> task = () -> {
                ready.countDown();
                go.await();
                return send(post("/api/v1/invoices"), "emp-sales", body)
                        .andReturn()
                        .getResponse()
                        .getStatus();
            };
            results.add(executor.submit(task));
        }
        ready.await();
        go.countDown();
        int succeeded = 0;
        for (Future<Integer> result : results) {
            if (result.get() == 200) {
                succeeded++;
            }
        }
        executor.shutdown();

        assertThat(succeeded).isEqualTo(2);
        assertThat(remaining("b-one")).isEqualTo(2);
        assertThat(invoiceRepository.count()).isEqualTo(2);
        assertThat(movementRepository.count()).isEqualTo(2);
    }

    @Test
    void unsellableMedicinesAreRejected() throws Exception {
        send(post("/api/v1/invoices"), "emp-sales", invoiceBody("med-unv", 1, ""))
                .andExpect(status().isConflict());
        send(post("/api/v1/invoices"), "emp-sales", invoiceBody("med-off", 1, ""))
                .andExpect(status().isConflict());
        send(post("/api/v1/invoices"), "emp-sales", invoiceBody("missing", 1, ""))
                .andExpect(status().isNotFound());

        assertThat(invoiceRepository.count()).isZero();
    }

    @Test
    void prescriptionRulesAreEnforced() throws Exception {
        send(post("/api/v1/invoices"), "emp-sales", invoiceBody("med-rx", 1, ""))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("PRESCRIPTION_REQUIRED"));
        send(post("/api/v1/invoices"), "emp-sales", invoiceBody("med-rx", 1, ",\"prescriptionConfirmed\":true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prescriptionConfirmed").value(true));
        send(post("/api/v1/invoices"), "emp-sales", invoiceBody("med-ctl", 1, ",\"prescriptionConfirmed\":true"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fieldErrors.prescriptionCode").exists())
                .andExpect(jsonPath("$.fieldErrors.customerId").exists());
        send(
                        post("/api/v1/invoices"),
                        "emp-sales",
                        invoiceBody(
                                "med-ctl",
                                1,
                                ",\"prescriptionConfirmed\":true,\"prescriptionCode\":\"DT-001\",\"customerId\":\"cus-1\""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prescriptionCode").value("DT-001"))
                .andExpect(jsonPath("$.customerName").value("Nguyễn Văn A"));

        assertThat(invoiceRepository.count()).isEqualTo(2);
    }

    @Test
    void paymentAndDiscountRulesAreValidated() throws Exception {
        send(
                        post("/api/v1/invoices"),
                        "emp-sales",
                        "{\"items\":[{\"medicineId\":\"med-otc\",\"quantity\":2}],"
                                + "\"paymentMethod\":\"CASH\",\"receivedAmount\":3999}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.receivedAmount").exists());
        send(
                        post("/api/v1/invoices"),
                        "emp-sales",
                        invoiceBody("med-otc", 2, ",\"discountType\":\"AMOUNT\",\"discountValue\":4001"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.discountValue").exists());
        send(
                        post("/api/v1/invoices"),
                        "emp-sales",
                        invoiceBody("med-otc", 2, ",\"discountType\":\"PERCENT\",\"discountValue\":101"))
                .andExpect(status().isBadRequest());
        send(
                        post("/api/v1/invoices"),
                        "emp-sales",
                        "{\"items\":[{\"medicineId\":\"med-otc\",\"quantity\":2}]," + "\"paymentMethod\":\"TRANSFER\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.receivedAmount").value(4000))
                .andExpect(jsonPath("$.changeAmount").value(0));
    }

    @Test
    void onlySalesCanCheckoutAndPreviewTheNumber() throws Exception {
        send(post("/api/v1/invoices"), "emp-admin", invoiceBody("med-otc", 1, ""))
                .andExpect(status().isForbidden());
        send(post("/api/v1/invoices"), "emp-kho", invoiceBody("med-otc", 1, "")).andExpect(status().isForbidden());
        send(get("/api/v1/invoices/next-number"), "emp-kho", null).andExpect(status().isForbidden());
        send(get("/api/v1/invoices/next-number"), "emp-sales", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(org.hamcrest.Matchers.startsWith("HD-")));
    }

    @Test
    void draftsBelongToTheirOwnerAndAreRemovedAfterCheckout() throws Exception {
        String draftBody = "{\"customerId\":\"cus-1\",\"items\":[{\"medicineId\":\"med-otc\",\"quantity\":2}],"
                + "\"discountType\":\"AMOUNT\",\"discountValue\":0,\"paymentMethod\":\"CASH\"}";
        String saved = send(post("/api/v1/invoice-drafts"), "emp-sales", draftBody)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerName").value("Nguyễn Văn A"))
                .andExpect(jsonPath("$.items[0].medicineName").value("Paracetamol"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String draftId = com.jayway.jsonpath.JsonPath.read(saved, "$.id");

        send(get("/api/v1/invoice-drafts"), "emp-sales", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        send(get("/api/v1/invoice-drafts"), "emp-sales2", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        send(delete("/api/v1/invoice-drafts/" + draftId), "emp-sales2", null).andExpect(status().isNotFound());
        send(
                        post("/api/v1/invoice-drafts"),
                        "emp-sales2",
                        draftBody.replaceFirst("\\{", "{\"id\":\"" + draftId + "\","))
                .andExpect(status().isNotFound());

        send(post("/api/v1/invoices"), "emp-sales", invoiceBody("med-otc", 2, ",\"draftId\":\"" + draftId + "\""))
                .andExpect(status().isOk());

        assertThat(draftRepository.count()).isZero();
    }

    @Test
    void draftCanBeDeletedByItsOwner() throws Exception {
        String saved = send(
                        post("/api/v1/invoice-drafts"),
                        "emp-sales",
                        "{\"items\":[{\"medicineId\":\"med-otc\",\"quantity\":1}],\"paymentMethod\":\"CARD\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerName").value("Khách lẻ"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String draftId = com.jayway.jsonpath.JsonPath.read(saved, "$.id");

        send(delete("/api/v1/invoice-drafts/" + draftId), "emp-sales", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertThat(draftRepository.count()).isZero();
    }

    @Test
    void salesDashboardCountsOnlyTheCallersInvoicesToday() throws Exception {
        send(post("/api/v1/invoices"), "emp-sales", invoiceBody("med-otc", 2, ""))
                .andExpect(status().isOk());
        send(post("/api/v1/invoices"), "emp-sales2", invoiceBody("med-otc", 1, ""))
                .andExpect(status().isOk());
        send(
                        post("/api/v1/invoice-drafts"),
                        "emp-sales",
                        "{\"items\":[{\"medicineId\":\"med-otc\",\"quantity\":1}]," + "\"paymentMethod\":\"CASH\"}")
                .andExpect(status().isOk());

        send(get("/api/v1/dashboard/sales"), "emp-sales", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invoicesToday").value(1))
                .andExpect(jsonPath("$.revenueToday").value(4000))
                .andExpect(jsonPath("$.draftCount").value(1));
        send(get("/api/v1/dashboard/sales"), "emp-admin", null).andExpect(status().isForbidden());
    }

    @Test
    void salesSeesOnlyOwnInvoicesAfterCheckout() throws Exception {
        String created = send(post("/api/v1/invoices"), "emp-sales", invoiceBody("med-otc", 1, ""))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String invoiceId = com.jayway.jsonpath.JsonPath.read(created, "$.id");

        send(get("/api/v1/invoices/" + invoiceId), "emp-sales", null).andExpect(status().isOk());
        send(get("/api/v1/invoices/" + invoiceId), "emp-sales2", null).andExpect(status().isNotFound());
    }
}
