package com.pharmacy.demo.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pharmacy.demo.entity.Disposal;
import com.pharmacy.demo.entity.Employee;
import com.pharmacy.demo.entity.Invoice;
import com.pharmacy.demo.entity.StockBatch;
import com.pharmacy.demo.entity.StockMovement;
import com.pharmacy.demo.entity.Stocktake;
import com.pharmacy.demo.entity.embedded.BatchAllocation;
import com.pharmacy.demo.entity.embedded.DisposalLine;
import com.pharmacy.demo.entity.embedded.InvoiceLine;
import com.pharmacy.demo.entity.embedded.StocktakeLine;
import com.pharmacy.demo.enums.BatchHandlingStatus;
import com.pharmacy.demo.enums.DisposalReason;
import com.pharmacy.demo.enums.DisposalStatus;
import com.pharmacy.demo.enums.EmployeeStatus;
import com.pharmacy.demo.enums.InvoiceStatus;
import com.pharmacy.demo.enums.MovementType;
import com.pharmacy.demo.enums.PaymentMethod;
import com.pharmacy.demo.enums.PrescriptionType;
import com.pharmacy.demo.enums.Role;
import com.pharmacy.demo.enums.StocktakeStatus;
import com.pharmacy.demo.repository.DisposalRepository;
import com.pharmacy.demo.repository.EmployeeRepository;
import com.pharmacy.demo.repository.InvoiceRepository;
import com.pharmacy.demo.repository.StockBatchRepository;
import com.pharmacy.demo.repository.StockMovementRepository;
import com.pharmacy.demo.repository.StocktakeRepository;
import com.pharmacy.demo.security.JwtService;
import com.pharmacy.demo.support.MongoIntegrationTest;
import jakarta.servlet.http.Cookie;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
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
class CrossReviewApiIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private StockBatchRepository batchRepository;

    @Autowired
    private StockMovementRepository movementRepository;

    @Autowired
    private StocktakeRepository stocktakeRepository;

    @Autowired
    private DisposalRepository disposalRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private JwtService jwtService;

    @BeforeEach
    void seed() {
        movementRepository.deleteAll();
        batchRepository.deleteAll();
        stocktakeRepository.deleteAll();
        disposalRepository.deleteAll();
        invoiceRepository.deleteAll();
        employeeRepository.deleteAll();
        employeeRepository.save(employee("emp-admin", "admin", Role.ADMIN));
        employeeRepository.save(employee("emp-sales", "sales", Role.SALES));
        employeeRepository.save(employee("emp-sales2", "sales2", Role.SALES));
        employeeRepository.save(employee("emp-kho", "kho", Role.WAREHOUSE));
        batchRepository.save(batch("b-1", "L001", 100, 30, 3L, BatchHandlingStatus.AVAILABLE));
        batchRepository.save(batch("b-2", "L002", 50, 20, 1L, BatchHandlingStatus.AVAILABLE));
        batchRepository.save(batch("b-3", "L003", 10, 0, 2L, BatchHandlingStatus.DISPOSED));
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

    private StockBatch batch(
            String id, String code, int initial, int remaining, long version, BatchHandlingStatus handling) {
        return StockBatch.builder()
                .id(id)
                .code(code)
                .medicineId("med-1")
                .receiptId("rec-1")
                .receiptLineNo(Integer.parseInt(id.substring(2)))
                .supplierId("sup-1")
                .receivedDate(LocalDate.now().minusDays(5))
                .expiryDate(LocalDate.now().plusDays(400))
                .initialQty(initial)
                .remainingQty(remaining)
                .purchasePrice(1000L)
                .handlingStatus(handling)
                .ledgerVersion(version)
                .createdAt(Instant.now())
                .build();
    }

    private Stocktake stocktake(String id, String code, StocktakeLine... lines) {
        return Stocktake.builder()
                .id(id)
                .code(code)
                .scopeLabel("Toàn bộ kho")
                .createdById("emp-kho")
                .createdByName("Người dùng kho")
                .lines(List.of(lines))
                .status(StocktakeStatus.PENDING_APPROVAL)
                .createdAt(Instant.now())
                .submittedAt(Instant.now())
                .build();
    }

    private StocktakeLine line(String batchId, String code, int system, Integer actual, long version) {
        return StocktakeLine.builder()
                .batchId(batchId)
                .batchCode(code)
                .medicineId("med-1")
                .medicineName("Paracetamol")
                .systemQty(system)
                .actualQty(actual)
                .difference(actual == null ? 0 : actual - system)
                .reason("Hao hụt")
                .batchVersion(version)
                .build();
    }

    private Disposal disposal(String id, String code) {
        return Disposal.builder()
                .id(id)
                .code(code)
                .createdById("emp-kho")
                .createdByName("Người dùng kho")
                .reason("Thuốc hết hạn")
                .lines(List.of(DisposalLine.builder()
                        .batchId("b-1")
                        .batchCode("L001")
                        .medicineId("med-1")
                        .medicineName("Paracetamol")
                        .expiryDate(LocalDate.now().plusDays(400))
                        .reasonType(DisposalReason.EXPIRED)
                        .quantity(10)
                        .build()))
                .status(DisposalStatus.PENDING_APPROVAL)
                .createdAt(Instant.now())
                .submittedAt(Instant.now())
                .build();
    }

    private Invoice invoice(String id, String code, String employeeId, String customerId) {
        return Invoice.builder()
                .id(id)
                .code(code)
                .employeeId(employeeId)
                .employeeName("Người bán")
                .customerId(customerId)
                .customerName(customerId == null ? "Khách lẻ" : "Khách A")
                .items(List.of(InvoiceLine.builder()
                        .medicineId("med-1")
                        .medicineCode("T0001")
                        .medicineName("Paracetamol")
                        .unit("Viên")
                        .prescriptionType(PrescriptionType.OTC)
                        .quantity(5)
                        .unitPrice(2000L)
                        .subtotal(10000L)
                        .allocations(List.of(
                                BatchAllocation.builder()
                                        .batchId("b-1")
                                        .batchCode("L001")
                                        .quantity(3)
                                        .build(),
                                BatchAllocation.builder()
                                        .batchId("b-3")
                                        .batchCode("L003")
                                        .quantity(2)
                                        .build()))
                        .build()))
                .subtotal(10000L)
                .total(10000L)
                .paymentMethod(PaymentMethod.CASH)
                .status(InvoiceStatus.COMPLETED)
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

    @Test
    void approvingAStocktakeAdjustsStockAndRecordsMovements() throws Exception {
        stocktakeRepository.save(
                stocktake("stk-1", "KK-2026-001", line("b-1", "L001", 30, 28, 3), line("b-2", "L002", 20, 20, 1)));

        send(post("/api/v1/stocktakes/stk-1/approve"), "emp-admin", "{\"note\":\"Khớp\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.reviewNote").value("Khớp"))
                .andExpect(jsonPath("$.reviewedByName").value("Người dùng admin"));

        StockBatch adjusted = batchRepository.findById("b-1").orElseThrow();
        assertThat(adjusted.getRemainingQty()).isEqualTo(28);
        assertThat(adjusted.getLedgerVersion()).isEqualTo(4L);
        assertThat(batchRepository.findById("b-2").orElseThrow().getLedgerVersion())
                .isEqualTo(1L);
        List<StockMovement> movements = movementRepository.findAll();
        assertThat(movements).hasSize(1);
        assertThat(movements.get(0).getType()).isEqualTo(MovementType.ADJUSTMENT);
        assertThat(movements.get(0).getQuantity()).isEqualTo(-2);
        assertThat(movements.get(0).getReferenceCode()).isEqualTo("KK-2026-001");
        assertThat(movements.get(0).getBalanceAfter()).isEqualTo(28);

        send(post("/api/v1/stocktakes/stk-1/approve"), "emp-admin", null).andExpect(status().isConflict());
        assertThat(batchRepository.findById("b-1").orElseThrow().getRemainingQty())
                .isEqualTo(28);
    }

    @Test
    void stocktakeApprovalIsRejectedAndRolledBackWhenABatchChangedSinceCounting() throws Exception {
        stocktakeRepository.save(
                stocktake("stk-1", "KK-2026-001", line("b-1", "L001", 30, 25, 3), line("b-2", "L002", 20, 18, 0)));

        send(post("/api/v1/stocktakes/stk-1/approve"), "emp-admin", null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("L002")));

        assertThat(batchRepository.findById("b-1").orElseThrow().getRemainingQty())
                .isEqualTo(30);
        assertThat(batchRepository.findById("b-2").orElseThrow().getRemainingQty())
                .isEqualTo(20);
        assertThat(movementRepository.count()).isZero();
        assertThat(stocktakeRepository.findById("stk-1").orElseThrow().getStatus())
                .isEqualTo(StocktakeStatus.PENDING_APPROVAL);
    }

    @Test
    void stocktakeRejectionNeedsANoteAndLeavesStockUntouched() throws Exception {
        stocktakeRepository.save(stocktake("stk-1", "KK-2026-001", line("b-1", "L001", 30, 10, 3)));

        send(post("/api/v1/stocktakes/stk-1/reject"), "emp-admin", "{\"note\":\"\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.note").exists());
        send(post("/api/v1/stocktakes/stk-1/reject"), "emp-admin", "{\"note\":\"Đếm lại\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.reviewNote").value("Đếm lại"));
        assertThat(batchRepository.findById("b-1").orElseThrow().getRemainingQty())
                .isEqualTo(30);
        send(post("/api/v1/stocktakes/stk-1/reject"), "emp-admin", "{\"note\":\"Lại\"}")
                .andExpect(status().isConflict());
    }

    @Test
    void stocktakesAreReadableByWarehouseButOnlyAdminsReview() throws Exception {
        stocktakeRepository.save(stocktake("stk-1", "KK-2026-001", line("b-1", "L001", 30, 28, 3)));

        send(get("/api/v1/stocktakes"), "emp-kho", null)
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].scope").value("ALL"))
                .andExpect(jsonPath("$.items[0].lineCount").value(1))
                .andExpect(jsonPath("$.items[0].differenceCount").value(1))
                .andExpect(jsonPath("$.items[0].lines").doesNotExist());
        send(get("/api/v1/stocktakes/stk-1"), "emp-admin", null)
                .andExpect(jsonPath("$.lines[0].difference").value(-2));
        send(get("/api/v1/stocktakes").param("status", "APPROVED"), "emp-admin", null)
                .andExpect(jsonPath("$.total").value(0));
        send(post("/api/v1/stocktakes/stk-1/approve"), "emp-kho", null).andExpect(status().isForbidden());
        send(get("/api/v1/stocktakes"), "emp-sales", null).andExpect(status().isForbidden());
        send(get("/api/v1/stocktakes/missing"), "emp-admin", null).andExpect(status().isNotFound());
    }

    @Test
    void approvingADisposalDoesNotTouchStockButRejectingReleasesTheBatches() throws Exception {
        batchRepository.save(batch("b-1", "L001", 100, 30, 3L, BatchHandlingStatus.QUARANTINED));
        disposalRepository.save(disposal("dsp-1", "XH-2026-001"));
        disposalRepository.save(disposal("dsp-2", "XH-2026-002"));

        send(post("/api/v1/disposals/dsp-1/approve"), "emp-admin", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.reviewNote").value("Đã duyệt"));
        StockBatch afterApprove = batchRepository.findById("b-1").orElseThrow();
        assertThat(afterApprove.getRemainingQty()).isEqualTo(30);
        assertThat(afterApprove.getHandlingStatus()).isEqualTo(BatchHandlingStatus.QUARANTINED);
        assertThat(movementRepository.count()).isZero();

        send(post("/api/v1/disposals/dsp-2/reject"), "emp-admin", "{\"note\":\"Còn dùng được\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
        assertThat(batchRepository.findById("b-1").orElseThrow().getHandlingStatus())
                .isEqualTo(BatchHandlingStatus.AVAILABLE);
        send(post("/api/v1/disposals/dsp-2/approve"), "emp-admin", null).andExpect(status().isConflict());
    }

    @Test
    void disposalsAreVisibleToAdminAndWarehouseOnly() throws Exception {
        disposalRepository.save(disposal("dsp-1", "XH-2026-001"));

        send(get("/api/v1/disposals"), "emp-kho", null)
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].totalQuantity").value(10))
                .andExpect(jsonPath("$.items[0].lineCount").value(1));
        send(get("/api/v1/disposals/dsp-1"), "emp-admin", null)
                .andExpect(jsonPath("$.lines[0].reasonType").value("EXPIRED"));
        send(get("/api/v1/disposals"), "emp-sales", null).andExpect(status().isForbidden());
        send(post("/api/v1/disposals/dsp-1/approve"), "emp-kho", null).andExpect(status().isForbidden());
        send(get("/api/v1/disposals/missing"), "emp-admin", null).andExpect(status().isNotFound());
    }

    @Test
    void cancellingAnInvoiceReturnsStockToTheOriginalBatchesExactlyOnce() throws Exception {
        invoiceRepository.save(invoice("inv-1", "HD-20261030-0001", "emp-sales", null));

        send(post("/api/v1/invoices/inv-1/cancel"), "emp-admin", "{\"reason\":\"\"}")
                .andExpect(status().isBadRequest());
        send(post("/api/v1/invoices/inv-1/cancel"), "emp-admin", "{\"reason\":\"Khách đổi ý\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancelReason").value("Khách đổi ý"))
                .andExpect(jsonPath("$.cancelledByName").value("Người dùng admin"));

        assertThat(batchRepository.findById("b-1").orElseThrow().getRemainingQty())
                .isEqualTo(33);
        StockBatch disposedBatch = batchRepository.findById("b-3").orElseThrow();
        assertThat(disposedBatch.getRemainingQty()).isEqualTo(2);
        assertThat(disposedBatch.getHandlingStatus()).isEqualTo(BatchHandlingStatus.QUARANTINED);
        List<StockMovement> movements = movementRepository.findAll();
        assertThat(movements).hasSize(2).allSatisfy(movement -> {
            assertThat(movement.getType()).isEqualTo(MovementType.RETURN);
            assertThat(movement.getReferenceCode()).isEqualTo("HD-20261030-0001");
        });

        send(post("/api/v1/invoices/inv-1/cancel"), "emp-admin", "{\"reason\":\"Lại\"}")
                .andExpect(status().isConflict());
        assertThat(batchRepository.findById("b-1").orElseThrow().getRemainingQty())
                .isEqualTo(33);
        send(post("/api/v1/invoices/inv-1/cancel"), "emp-sales", "{\"reason\":\"x\"}")
                .andExpect(status().isForbidden());
        send(post("/api/v1/invoices/missing/cancel"), "emp-admin", "{\"reason\":\"x\"}")
                .andExpect(status().isNotFound());
    }

    @Test
    void salesOnlySeesOwnInvoicesWhileAdminsSeeAll() throws Exception {
        invoiceRepository.save(invoice("inv-1", "HD-20261030-0001", "emp-sales", null));
        invoiceRepository.save(invoice("inv-2", "HD-20261030-0002", "emp-sales2", null));

        send(get("/api/v1/invoices"), "emp-sales", null)
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].id").value("inv-1"))
                .andExpect(jsonPath("$.items[0].itemCount").value(1))
                .andExpect(jsonPath("$.items[0].quantityTotal").value(5));
        send(get("/api/v1/invoices").param("employeeId", "emp-sales2"), "emp-sales", null)
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].id").value("inv-1"));
        send(get("/api/v1/invoices/inv-2"), "emp-sales", null).andExpect(status().isNotFound());
        send(get("/api/v1/invoices/inv-1"), "emp-sales", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].allocations.length()").value(2))
                .andExpect(jsonPath("$.employee.name").value("Người dùng sales"))
                .andExpect(jsonPath("$.customer").doesNotExist());

        send(get("/api/v1/invoices"), "emp-admin", null)
                .andExpect(jsonPath("$.total").value(2));
        send(get("/api/v1/invoices").param("employeeId", "emp-sales2"), "emp-admin", null)
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].id").value("inv-2"));
        send(get("/api/v1/invoices").param("mine", "true"), "emp-admin", null)
                .andExpect(jsonPath("$.total").value(0));
        send(get("/api/v1/invoices").param("status", "CANCELLED"), "emp-admin", null)
                .andExpect(jsonPath("$.total").value(0));
        send(get("/api/v1/invoices"), "emp-kho", null).andExpect(status().isForbidden());
    }
}
