package com.pharmacy.demo.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.pharmacy.demo.entity.Employee;
import com.pharmacy.demo.entity.GoodsReceipt;
import com.pharmacy.demo.entity.Medicine;
import com.pharmacy.demo.entity.StockBatch;
import com.pharmacy.demo.entity.StockMovement;
import com.pharmacy.demo.entity.Supplier;
import com.pharmacy.demo.entity.embedded.ReceiptLine;
import com.pharmacy.demo.enums.BatchHandlingStatus;
import com.pharmacy.demo.enums.EmployeeStatus;
import com.pharmacy.demo.enums.MedicineStatus;
import com.pharmacy.demo.enums.MovementType;
import com.pharmacy.demo.enums.PrescriptionType;
import com.pharmacy.demo.enums.ReceiptStatus;
import com.pharmacy.demo.enums.ReferenceType;
import com.pharmacy.demo.enums.Role;
import com.pharmacy.demo.enums.SupplierStatus;
import com.pharmacy.demo.repository.EmployeeRepository;
import com.pharmacy.demo.repository.GoodsReceiptRepository;
import com.pharmacy.demo.repository.MedicineRepository;
import com.pharmacy.demo.repository.StockBatchRepository;
import com.pharmacy.demo.repository.StockMovementRepository;
import com.pharmacy.demo.repository.SupplierRepository;
import com.pharmacy.demo.security.JwtService;
import com.pharmacy.demo.support.MongoIntegrationTest;
import jakarta.servlet.http.Cookie;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@MongoIntegrationTest
@AutoConfigureMockMvc
class InventoryViewApiIT {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private MedicineRepository medicineRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private GoodsReceiptRepository receiptRepository;

    @Autowired
    private StockBatchRepository batchRepository;

    @Autowired
    private StockMovementRepository movementRepository;

    @Autowired
    private JwtService jwtService;

    private LocalDate today;

    @BeforeEach
    void seed() {
        today = LocalDate.now(ZONE);
        movementRepository.deleteAll();
        batchRepository.deleteAll();
        receiptRepository.deleteAll();
        supplierRepository.deleteAll();
        medicineRepository.deleteAll();
        employeeRepository.deleteAll();
        employeeRepository.save(employee("emp-admin", "admin", Role.ADMIN));
        employeeRepository.save(employee("emp-sales", "sales", Role.SALES));
        employeeRepository.save(employee("emp-kho", "kho", Role.WAREHOUSE));
        supplierRepository.save(Supplier.builder()
                .id("sup-1")
                .code("NCC001")
                .name("Công ty Dược A")
                .status(SupplierStatus.ACTIVE)
                .createdAt(Instant.now())
                .build());
        medicineRepository.save(medicine("med-1", "T0001", "Paracetamol 500"));
        medicineRepository.save(medicine("med-2", "T0002", "Amoxicillin 500"));
        receiptRepository.save(receipt("rec-1", "PN-2026-0001", ReceiptStatus.CONFIRMED, today.minusDays(5)));
        receiptRepository.save(receipt("rec-2", "PN-2026-0002", ReceiptStatus.DRAFT, today.minusDays(40)));
        batchRepository.save(batch("b-1", "L001", 100, 30, today.plusDays(1000)));
        batchRepository.save(batch("b-2", "L002", 50, 5, today.plusDays(10)));
        batchRepository.save(batch("b-3", "L003", 20, 7, today.minusDays(5)));
        batchRepository.save(batch("b-4", "L004", 40, 0, today.plusDays(300)));
        movementRepository.save(movement("m-1", "b-1", "L001", MovementType.IN, 100, daysAgo(2)));
        movementRepository.save(movement("m-2", "b-1", "L001", MovementType.OUT, -70, daysAgo(0)));
        movementRepository.save(movement("m-3", "b-2", "L002", MovementType.ADJUSTMENT, -3, daysAgo(10)));
    }

    private Instant daysAgo(int days) {
        return today.minusDays(days).atTime(12, 0).atZone(ZONE).toInstant();
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

    private Medicine medicine(String id, String code, String name) {
        return Medicine.builder()
                .id(id)
                .code(code)
                .name(name)
                .unit("Viên")
                .categoryId("cat-1")
                .price(2000L)
                .costPrice(1000L)
                .minStock(10)
                .prescriptionType(PrescriptionType.OTC)
                .status(MedicineStatus.ACTIVE)
                .createdAt(Instant.now())
                .build();
    }

    private GoodsReceipt receipt(String id, String code, ReceiptStatus status, LocalDate date) {
        return GoodsReceipt.builder()
                .id(id)
                .code(code)
                .supplierId("sup-1")
                .supplierName("Công ty Dược A")
                .createdById("emp-kho")
                .createdByName("Người dùng kho")
                .receivingDate(date)
                .items(List.of(ReceiptLine.builder()
                        .lineNo(1)
                        .medicineId("med-1")
                        .medicineCode("T0001")
                        .medicineName("Paracetamol 500")
                        .batchCode("L001")
                        .quantity(100)
                        .unit("Viên")
                        .purchasePrice(1000L)
                        .subtotal(100000L)
                        .build()))
                .totalQuantity(100)
                .totalAmount(100000L)
                .status(status)
                .createdAt(Instant.now())
                .build();
    }

    private StockBatch batch(String id, String code, int initial, int remaining, LocalDate expiry) {
        return StockBatch.builder()
                .id(id)
                .code(code)
                .medicineId("med-1")
                .receiptId("rec-1")
                .receiptLineNo(Integer.parseInt(id.substring(2)))
                .supplierId("sup-1")
                .receivedDate(today.minusDays(5))
                .expiryDate(expiry)
                .initialQty(initial)
                .remainingQty(remaining)
                .purchasePrice(1000L)
                .handlingStatus(BatchHandlingStatus.AVAILABLE)
                .createdAt(Instant.now())
                .build();
    }

    private StockMovement movement(
            String id, String batchId, String batchCode, MovementType type, int quantity, Instant at) {
        return StockMovement.builder()
                .id(id)
                .medicineId("med-1")
                .medicineCode("T0001")
                .medicineName("Paracetamol 500")
                .batchId(batchId)
                .batchCode(batchCode)
                .type(type)
                .quantity(quantity)
                .balanceAfter(0)
                .referenceType(ReferenceType.RECEIPT)
                .referenceCode("PN-2026-0001")
                .performedById("emp-kho")
                .performedByName("Người dùng kho")
                .createdAt(at)
                .build();
    }

    private ResultActions send(MockHttpServletRequestBuilder builder, String actorId) throws Exception {
        Employee employee = employeeRepository.findById(actorId).orElseThrow();
        return mockMvc.perform(
                builder.cookie(new Cookie("PMS_TOKEN", jwtService.issue(employee, Duration.ofMinutes(10)))));
    }

    @Test
    void receiptsCanBeFilteredAndOpened() throws Exception {
        send(get("/api/v1/receipts"), "emp-admin")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.items[0].code").value("PN-2026-0001"))
                .andExpect(jsonPath("$.items[0].itemCount").value(1));
        send(get("/api/v1/receipts").param("status", "DRAFT"), "emp-kho")
                .andExpect(jsonPath("$.total").value(1));
        send(get("/api/v1/receipts").param("supplierId", "other"), "emp-kho")
                .andExpect(jsonPath("$.total").value(0));
        send(get("/api/v1/receipts").param("from", today.minusDays(10).toString()), "emp-admin")
                .andExpect(jsonPath("$.total").value(1));
        send(get("/api/v1/receipts").param("q", "dược a"), "emp-admin")
                .andExpect(jsonPath("$.total").value(2));
        send(get("/api/v1/receipts/rec-1"), "emp-admin")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].medicineName").value("Paracetamol 500"))
                .andExpect(jsonPath("$.items[0].subtotal").value(100000))
                .andExpect(jsonPath("$.receivingDate").value(today.minusDays(5).toString()));
        send(get("/api/v1/receipts/missing"), "emp-admin").andExpect(status().isNotFound());
        send(get("/api/v1/receipts"), "emp-sales").andExpect(status().isForbidden());
    }

    @Test
    void batchesAreFilteredAndStatusIsComputed() throws Exception {
        send(get("/api/v1/batches"), "emp-admin")
                .andExpect(jsonPath("$.total").value(4))
                .andExpect(jsonPath("$.items[0].code").value("L003"))
                .andExpect(jsonPath("$.items[0].status").value("EXPIRED"))
                .andExpect(jsonPath("$.items[0].medicineName").value("Paracetamol 500"))
                .andExpect(jsonPath("$.items[0].supplierName").value("Công ty Dược A"));
        send(get("/api/v1/batches").param("hasStock", "true"), "emp-kho")
                .andExpect(jsonPath("$.total").value(3));
        send(get("/api/v1/batches").param("expiryWithin", "30"), "emp-kho")
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].code").value("L002"));
        send(get("/api/v1/batches").param("status", "OUT_OF_STOCK"), "emp-kho")
                .andExpect(jsonPath("$.total").value(1));
        send(get("/api/v1/batches").param("q", "l004"), "emp-kho")
                .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void salesSeesOnlyBatchesWithStockAndNoCost() throws Exception {
        send(get("/api/v1/batches"), "emp-sales")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.items[0].purchasePrice").doesNotExist());
        send(get("/api/v1/batches/b-1"), "emp-sales").andExpect(status().isForbidden());
    }

    @Test
    void batchDetailIncludesItsMovementsNewestFirst() throws Exception {
        String body = send(get("/api/v1/batches/b-1"), "emp-admin")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.purchasePrice").value(1000))
                .andExpect(jsonPath("$.movements.length()").value(2))
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(JsonPath.<String>read(body, "$.movements[0].type")).isEqualTo("OUT");
        send(get("/api/v1/batches/missing"), "emp-admin").andExpect(status().isNotFound());
    }

    @Test
    void movementsAreFilteredByTypeBatchAndDateRange() throws Exception {
        send(get("/api/v1/movements"), "emp-admin")
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.items[0].type").value("OUT"));
        send(get("/api/v1/movements").param("type", "OUT"), "emp-kho")
                .andExpect(jsonPath("$.total").value(1));
        send(get("/api/v1/movements").param("batchId", "b-1"), "emp-kho")
                .andExpect(jsonPath("$.total").value(2));
        send(
                        get("/api/v1/movements")
                                .param("from", today.minusDays(3).toString())
                                .param("to", today.toString()),
                        "emp-kho")
                .andExpect(jsonPath("$.total").value(2));
        send(get("/api/v1/movements").param("to", today.minusDays(3).toString()), "emp-kho")
                .andExpect(jsonPath("$.total").value(1));
        send(get("/api/v1/movements").param("q", "paracetamol"), "emp-kho")
                .andExpect(jsonPath("$.total").value(3));
        send(get("/api/v1/movements"), "emp-sales").andExpect(status().isForbidden());
    }

    @Test
    void inventorySummaryAddsUpStockAndWarnings() throws Exception {
        send(get("/api/v1/inventory/summary"), "emp-admin")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMedicines").value(2))
                .andExpect(jsonPath("$.activeMedicines").value(2))
                .andExpect(jsonPath("$.totalBatches").value(3))
                .andExpect(jsonPath("$.totalUnits").value(42))
                .andExpect(jsonPath("$.stockValue").value(42000))
                .andExpect(jsonPath("$.outOfStock").value(1))
                .andExpect(jsonPath("$.expiringBatches").value(1))
                .andExpect(jsonPath("$.expiredBatches").value(1));
        send(get("/api/v1/inventory/summary"), "emp-sales").andExpect(status().isForbidden());
    }
}
