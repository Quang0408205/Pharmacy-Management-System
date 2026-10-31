package com.pharmacy.demo.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pharmacy.demo.entity.ActivityLog;
import com.pharmacy.demo.entity.Category;
import com.pharmacy.demo.entity.Employee;
import com.pharmacy.demo.entity.Invoice;
import com.pharmacy.demo.entity.Medicine;
import com.pharmacy.demo.entity.StockBatch;
import com.pharmacy.demo.entity.embedded.InvoiceLine;
import com.pharmacy.demo.enums.BatchHandlingStatus;
import com.pharmacy.demo.enums.CategoryStatus;
import com.pharmacy.demo.enums.EmployeeStatus;
import com.pharmacy.demo.enums.InvoiceStatus;
import com.pharmacy.demo.enums.MedicineStatus;
import com.pharmacy.demo.enums.PaymentMethod;
import com.pharmacy.demo.enums.PrescriptionType;
import com.pharmacy.demo.enums.Role;
import com.pharmacy.demo.repository.ActivityLogRepository;
import com.pharmacy.demo.repository.CategoryRepository;
import com.pharmacy.demo.repository.EmployeeRepository;
import com.pharmacy.demo.repository.InvoiceRepository;
import com.pharmacy.demo.repository.MedicineRepository;
import com.pharmacy.demo.repository.StockBatchRepository;
import com.pharmacy.demo.repository.SystemSettingRepository;
import com.pharmacy.demo.security.JwtService;
import com.pharmacy.demo.support.MongoIntegrationTest;
import jakarta.servlet.http.Cookie;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
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
class StatsAndSystemApiIT {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private MedicineRepository medicineRepository;

    @Autowired
    private StockBatchRepository batchRepository;

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Autowired
    private SystemSettingRepository settingRepository;

    @Autowired
    private JwtService jwtService;

    private LocalDate today;

    @BeforeEach
    void seed() {
        today = LocalDate.now(ZONE);
        settingRepository.deleteAll();
        activityLogRepository.deleteAll();
        invoiceRepository.deleteAll();
        batchRepository.deleteAll();
        medicineRepository.deleteAll();
        categoryRepository.deleteAll();
        employeeRepository.deleteAll();
        employeeRepository.save(employee("emp-admin", "admin", Role.ADMIN));
        employeeRepository.save(employee("emp-sales", "sales", Role.SALES));
        employeeRepository.save(employee("emp-sales2", "sales2", Role.SALES));
        employeeRepository.save(employee("emp-kho", "kho", Role.WAREHOUSE));
        categoryRepository.save(category("cat-1", "KS", "Kháng sinh"));
        categoryRepository.save(category("cat-2", "TM", "Tim mạch"));
        medicineRepository.save(medicine("med-1", "T0001", "Paracetamol", "cat-1", 10));
        medicineRepository.save(medicine("med-2", "T0002", "Amlodipin", "cat-2", 10));
        batchRepository.save(batch("b-1", "med-1", 30, today.plusDays(400)));
        batchRepository.save(batch("b-2", "med-1", 5, today.plusDays(20)));
        invoiceRepository.save(invoice("inv-1", "emp-sales", "Người bán 1", InvoiceStatus.COMPLETED, 10000L, 5, 0));
        invoiceRepository.save(invoice("inv-2", "emp-sales2", "Người bán 2", InvoiceStatus.COMPLETED, 20000L, 3, 0));
        invoiceRepository.save(invoice("inv-3", "emp-sales", "Người bán 1", InvoiceStatus.COMPLETED, 5000L, 2, 1));
        invoiceRepository.save(invoice("inv-4", "emp-sales", "Người bán 1", InvoiceStatus.CANCELLED, 99000L, 9, 0));
    }

    @AfterEach
    void restoreDefaults() {
        settingRepository.deleteAll();
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

    private Category category(String id, String code, String name) {
        return Category.builder()
                .id(id)
                .code(code)
                .name(name)
                .color("#0d9488")
                .status(CategoryStatus.ACTIVE)
                .createdAt(Instant.now())
                .build();
    }

    private Medicine medicine(String id, String code, String name, String categoryId, int minStock) {
        return Medicine.builder()
                .id(id)
                .code(code)
                .name(name)
                .unit("Viên")
                .categoryId(categoryId)
                .price(2000L)
                .costPrice(1000L)
                .minStock(minStock)
                .prescriptionType(PrescriptionType.OTC)
                .status(MedicineStatus.ACTIVE)
                .createdAt(Instant.now())
                .build();
    }

    private StockBatch batch(String id, String medicineId, int remaining, LocalDate expiry) {
        return StockBatch.builder()
                .id(id)
                .code(id.toUpperCase())
                .medicineId(medicineId)
                .receiptId("rec-1")
                .receiptLineNo(Integer.parseInt(id.substring(2)))
                .supplierId("sup-1")
                .receivedDate(today.minusDays(3))
                .expiryDate(expiry)
                .initialQty(100)
                .remainingQty(remaining)
                .purchasePrice(1000L)
                .handlingStatus(BatchHandlingStatus.AVAILABLE)
                .createdAt(Instant.now())
                .build();
    }

    private Invoice invoice(
            String id,
            String employeeId,
            String employeeName,
            InvoiceStatus status,
            long total,
            int quantity,
            int daysAgo) {
        String medicineId = "inv-2".equals(id) ? "med-2" : "med-1";
        String medicineName = "inv-2".equals(id) ? "Amlodipin" : "Paracetamol";
        return Invoice.builder()
                .id(id)
                .code("HD-" + id)
                .employeeId(employeeId)
                .employeeName(employeeName)
                .customerName("Khách lẻ")
                .items(List.of(InvoiceLine.builder()
                        .medicineId(medicineId)
                        .medicineCode("med-1".equals(medicineId) ? "T0001" : "T0002")
                        .medicineName(medicineName)
                        .unit("Viên")
                        .prescriptionType(PrescriptionType.OTC)
                        .quantity(quantity)
                        .unitPrice(total / quantity)
                        .subtotal(total)
                        .allocations(List.of())
                        .build()))
                .subtotal(total)
                .total(total)
                .paymentMethod(PaymentMethod.CASH)
                .status(status)
                .createdAt(Instant.now().minus(Duration.ofDays(daysAgo)))
                .build();
    }

    private ActivityLog log(String id, String actorName, Role role, String action, String description, int daysAgo) {
        return ActivityLog.builder()
                .id(id)
                .actorId("emp-x")
                .actorName(actorName)
                .role(role)
                .action(action)
                .entity("MEDICINE")
                .entityCode("T0001")
                .description(description)
                .createdAt(Instant.now().minus(Duration.ofDays(daysAgo)))
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
    void adminUpdatesSettingsPartiallyAndTheChangeIsLogged() throws Exception {
        send(
                        put("/api/v1/settings"),
                        "emp-admin",
                        "{\"pharmacy\":{\"name\":\"Nhà thuốc Mới\"},\"expiryTiers\":[15,45,120],"
                                + "\"lowStockMultiplier\":1.5,\"notifications\":{\"dailyReport\":true},"
                                + "\"appearance\":{\"theme\":\"dark\"}}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pharmacy.name").value("Nhà thuốc Mới"))
                .andExpect(jsonPath("$.pharmacy.taxCode").value("0312345678"))
                .andExpect(jsonPath("$.expiryTiers[2]").value(120))
                .andExpect(jsonPath("$.lowStockMultiplier").value(1.5))
                .andExpect(jsonPath("$.notifications.dailyReport").value(true))
                .andExpect(jsonPath("$.notifications.lowStock").value(true))
                .andExpect(jsonPath("$.appearance.theme").value("dark"))
                .andExpect(jsonPath("$.appearance.density").value("comfortable"))
                .andExpect(jsonPath("$.expiryWarningDays").value(90));

        send(get("/api/v1/settings"), "emp-sales", null)
                .andExpect(jsonPath("$.pharmacy.name").value("Nhà thuốc Mới"));
        assertThat(activityLogRepository.findAll())
                .anyMatch(
                        entry -> "UPDATE_SETTINGS".equals(entry.getAction()) && "emp-admin".equals(entry.getActorId()));
    }

    @Test
    void invalidSettingsAreRejectedWithFieldErrorsAndNothingIsSaved() throws Exception {
        send(put("/api/v1/settings"), "emp-admin", "{\"lowStockMultiplier\":9}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.lowStockMultiplier").exists());
        send(put("/api/v1/settings"), "emp-admin", "{\"expiryWarningDays\":3}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.expiryWarningDays").exists());
        send(put("/api/v1/settings"), "emp-admin", "{\"expiryTiers\":[60,30,90]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.expiryTiers").exists());
        send(put("/api/v1/settings"), "emp-admin", "{\"expiryTiers\":[30,60]}").andExpect(status().isBadRequest());
        send(put("/api/v1/settings"), "emp-admin", "{\"pharmacy\":{\"name\":\"   \"}}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors['pharmacy.name']").exists());
        send(put("/api/v1/settings"), "emp-admin", "{\"appearance\":{\"theme\":\"neon\"}}")
                .andExpect(status().isBadRequest());
        send(get("/api/v1/settings"), "emp-admin", null)
                .andExpect(jsonPath("$.pharmacy.name").value("Nhà thuốc Minh Tâm"))
                .andExpect(jsonPath("$.lowStockMultiplier").value(1.0));
        assertThat(activityLogRepository.count()).isZero();
    }

    @Test
    void onlyAdminsMayChangeSettings() throws Exception {
        send(put("/api/v1/settings"), "emp-sales", "{\"lowStockMultiplier\":2}").andExpect(status().isForbidden());
        send(put("/api/v1/settings"), "emp-kho", "{\"lowStockMultiplier\":2}").andExpect(status().isForbidden());
    }

    @Test
    void activityLogsAreFilteredSearchedAndPaged() throws Exception {
        activityLogRepository.save(
                log("l-1", "Trần Thị Hiền", Role.ADMIN, "CREATE_MEDICINE", "Thêm thuốc Paracetamol", 0));
        activityLogRepository.save(log("l-2", "Phạm Thị Lan", Role.SALES, "LOGIN", "Đăng nhập hệ thống", 1));
        activityLogRepository.save(
                log("l-3", "Hồ Văn Tùng", Role.WAREHOUSE, "CONFIRM_RECEIPT", "Xác nhận phiếu nhập", 10));

        send(get("/api/v1/activity-logs"), "emp-admin", null)
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.items[0].id").value("l-1"))
                .andExpect(jsonPath("$.items[2].id").value("l-3"));
        send(get("/api/v1/activity-logs").param("role", "SALES"), "emp-admin", null)
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].actorName").value("Phạm Thị Lan"));
        send(get("/api/v1/activity-logs").param("q", "tran thi hien"), "emp-admin", null)
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].id").value("l-1"));
        send(get("/api/v1/activity-logs").param("from", today.minusDays(2).toString()), "emp-admin", null)
                .andExpect(jsonPath("$.total").value(2));
        send(get("/api/v1/activity-logs").param("to", today.minusDays(5).toString()), "emp-admin", null)
                .andExpect(jsonPath("$.total").value(1));
        send(get("/api/v1/activity-logs").param("size", "2").param("page", "2"), "emp-admin", null)
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.totalPages").value(2));
        send(get("/api/v1/activity-logs").param("sort", "actorName,asc"), "emp-admin", null)
                .andExpect(jsonPath("$.items[0].id").value("l-3"));
        send(get("/api/v1/activity-logs"), "emp-kho", null).andExpect(status().isForbidden());
    }

    @Test
    void reportsSummariseOnlyCompletedInvoicesInTheRange() throws Exception {
        send(
                        get("/api/v1/reports/summary")
                                .param("from", today.toString())
                                .param("to", today.toString()),
                        "emp-admin",
                        null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revenue").value(30000))
                .andExpect(jsonPath("$.invoices").value(2))
                .andExpect(jsonPath("$.cancelled").value(1))
                .andExpect(jsonPath("$.averageOrder").value(15000))
                .andExpect(jsonPath("$.itemsSold").value(8));
        send(get("/api/v1/reports/summary"), "emp-admin", null)
                .andExpect(jsonPath("$.revenue").value(35000))
                .andExpect(jsonPath("$.invoices").value(3))
                .andExpect(jsonPath("$.from").value(today.minusDays(29).toString()))
                .andExpect(jsonPath("$.to").value(today.toString()));
        send(
                        get("/api/v1/reports/summary")
                                .param("from", today.toString())
                                .param("to", today.minusDays(1).toString()),
                        "emp-admin",
                        null)
                .andExpect(status().isBadRequest());
        send(get("/api/v1/reports/summary").param("from", today.minusDays(500).toString()), "emp-admin", null)
                .andExpect(status().isBadRequest());
    }

    @Test
    void revenueSeriesHasOnePointPerDayIncludingEmptyOnes() throws Exception {
        send(get("/api/v1/reports/revenue"), "emp-admin", null)
                .andExpect(jsonPath("$.length()").value(30))
                .andExpect(jsonPath("$[29].date").value(today.toString()))
                .andExpect(jsonPath("$[29].revenue").value(30000))
                .andExpect(jsonPath("$[29].invoices").value(2))
                .andExpect(jsonPath("$[28].revenue").value(5000))
                .andExpect(jsonPath("$[27].revenue").value(0));
    }

    @Test
    void bestSellersAndEmployeeRevenueAreRankedAndCancelledInvoicesAreIgnored() throws Exception {
        send(get("/api/v1/reports/best-sellers"), "emp-admin", null)
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Paracetamol"))
                .andExpect(jsonPath("$[0].quantity").value(7))
                .andExpect(jsonPath("$[0].revenue").value(15000))
                .andExpect(jsonPath("$[1].name").value("Amlodipin"));
        send(get("/api/v1/reports/best-sellers").param("limit", "1"), "emp-admin", null)
                .andExpect(jsonPath("$.length()").value(1));
        send(get("/api/v1/reports/revenue-by-employee"), "emp-admin", null)
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].employeeId").value("emp-sales2"))
                .andExpect(jsonPath("$[0].revenue").value(20000))
                .andExpect(jsonPath("$[1].invoices").value(2));
    }

    @Test
    void inventoryDistributionListsCategoriesThatHaveMedicines() throws Exception {
        categoryRepository.save(category("cat-3", "VT", "Vitamin"));
        send(get("/api/v1/reports/inventory-distribution"), "emp-admin", null)
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].categoryId").value("cat-1"))
                .andExpect(jsonPath("$[0].medicines").value(1))
                .andExpect(jsonPath("$[0].quantity").value(35))
                .andExpect(jsonPath("$[0].value").value(35000))
                .andExpect(jsonPath("$[1].quantity").value(0));
    }

    @Test
    void adminDashboardCombinesKpisSeriesAndLists() throws Exception {
        activityLogRepository.save(log("l-1", "Trần Thị Hiền", Role.ADMIN, "LOGIN", "Đăng nhập", 0));
        send(get("/api/v1/dashboard/admin"), "emp-admin", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kpis.todayRevenue").value(30000))
                .andExpect(jsonPath("$.kpis.todayInvoices").value(2))
                .andExpect(jsonPath("$.kpis.activeMedicines").value(2))
                .andExpect(jsonPath("$.kpis.lowStockCount").value(1))
                .andExpect(jsonPath("$.kpis.expiringBatches").value(1))
                .andExpect(jsonPath("$.revenueSeries.length()").value(14))
                .andExpect(jsonPath("$.categoryDistribution.length()").value(2))
                .andExpect(jsonPath("$.recentInvoices.length()").value(4))
                .andExpect(jsonPath("$.lowStock[0].code").value("T0002"))
                .andExpect(jsonPath("$.expiring[0].code").value("B-2"))
                .andExpect(jsonPath("$.activities.length()").value(1));
        send(get("/api/v1/dashboard/admin"), "emp-sales", null).andExpect(status().isForbidden());
        send(get("/api/v1/reports/summary"), "emp-sales", null).andExpect(status().isForbidden());
    }
}
