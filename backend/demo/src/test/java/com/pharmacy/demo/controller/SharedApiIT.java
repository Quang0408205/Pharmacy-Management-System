package com.pharmacy.demo.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pharmacy.demo.entity.Category;
import com.pharmacy.demo.entity.Employee;
import com.pharmacy.demo.entity.GoodsReceipt;
import com.pharmacy.demo.entity.Medicine;
import com.pharmacy.demo.entity.StockBatch;
import com.pharmacy.demo.entity.StockMovement;
import com.pharmacy.demo.entity.Stocktake;
import com.pharmacy.demo.enums.BatchHandlingStatus;
import com.pharmacy.demo.enums.CategoryStatus;
import com.pharmacy.demo.enums.EmployeeStatus;
import com.pharmacy.demo.enums.MedicineStatus;
import com.pharmacy.demo.enums.MovementType;
import com.pharmacy.demo.enums.PrescriptionType;
import com.pharmacy.demo.enums.ReceiptStatus;
import com.pharmacy.demo.enums.ReferenceType;
import com.pharmacy.demo.enums.Role;
import com.pharmacy.demo.enums.StocktakeStatus;
import com.pharmacy.demo.repository.CategoryRepository;
import com.pharmacy.demo.repository.EmployeeRepository;
import com.pharmacy.demo.repository.GoodsReceiptRepository;
import com.pharmacy.demo.repository.MedicineRepository;
import com.pharmacy.demo.repository.StockBatchRepository;
import com.pharmacy.demo.repository.StockMovementRepository;
import com.pharmacy.demo.repository.StocktakeRepository;
import com.pharmacy.demo.repository.SystemSettingRepository;
import com.pharmacy.demo.security.JwtService;
import com.pharmacy.demo.support.MongoIntegrationTest;
import jakarta.servlet.http.Cookie;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@MongoIntegrationTest
@AutoConfigureMockMvc
class SharedApiIT {

    private static final LocalDate TODAY = LocalDate.now();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private CategoryRepository categoryRepository;

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
    private SystemSettingRepository settingRepository;

    private Map<Role, Cookie> cookies;

    @BeforeEach
    void seed() {
        movementRepository.deleteAll();
        batchRepository.deleteAll();
        medicineRepository.deleteAll();
        categoryRepository.deleteAll();
        receiptRepository.deleteAll();
        stocktakeRepository.deleteAll();
        employeeRepository.deleteAll();
        settingRepository.deleteAll();

        cookies = Map.of(
                Role.ADMIN, cookieFor("e-admin", Role.ADMIN),
                Role.SALES, cookieFor("e-sales", Role.SALES),
                Role.WAREHOUSE, cookieFor("e-wh", Role.WAREHOUSE));

        categoryRepository.save(category("cat-1", "GDHS", "Giảm đau - Hạ sốt"));
        categoryRepository.save(category("cat-2", "KSIN", "Kháng sinh"));

        medicineRepository.save(medicine(
                "m1",
                "T0001",
                "Paracetamol 500mg",
                "cat-1",
                18000L,
                12000L,
                PrescriptionType.OTC,
                MedicineStatus.ACTIVE,
                20));
        medicineRepository.save(medicine(
                "m2",
                "T0002",
                "Amoxicillin 500mg",
                "cat-2",
                45000L,
                30000L,
                PrescriptionType.RX,
                MedicineStatus.ACTIVE,
                10));
        medicineRepository.save(medicine(
                "m3",
                "T0003",
                "Ibuprofen 400mg",
                "cat-1",
                24000L,
                16000L,
                PrescriptionType.OTC,
                MedicineStatus.INACTIVE,
                10));

        batchRepository.save(batch("b1", "m1", "LOT1", TODAY.plusDays(200), 100, 50, BatchHandlingStatus.AVAILABLE, 1));
        batchRepository.save(batch("b2", "m1", "LOT2", TODAY.minusDays(10), 20, 5, BatchHandlingStatus.AVAILABLE, 2));
        batchRepository.save(
                batch("b3", "m1", "LOT3", TODAY.plusDays(300), 30, 30, BatchHandlingStatus.QUARANTINED, 3));
        batchRepository.save(batch("b4", "m2", "LOT4", TODAY.plusDays(20), 8, 8, BatchHandlingStatus.AVAILABLE, 4));
        batchRepository.save(batch("b5", "m3", "LOT5", TODAY.plusDays(100), 10, 0, BatchHandlingStatus.AVAILABLE, 5));

        movementRepository.save(StockMovement.builder()
                .id("mv-1")
                .medicineId("m1")
                .medicineName("Paracetamol 500mg")
                .batchId("b1")
                .batchCode("LOT1")
                .type(MovementType.IN)
                .quantity(100)
                .balanceAfter(100)
                .referenceType(ReferenceType.RECEIPT)
                .referenceCode("PN-1")
                .createdAt(Instant.now())
                .build());

        receiptRepository.save(GoodsReceipt.builder()
                .id("r1")
                .code("PN-2026-0001")
                .status(ReceiptStatus.DRAFT)
                .build());
        stocktakeRepository.save(Stocktake.builder()
                .id("s1")
                .code("KK-2026-001")
                .status(StocktakeStatus.PENDING_APPROVAL)
                .build());
    }

    private Cookie cookieFor(String id, Role role) {
        Employee employee = employeeRepository.save(Employee.builder()
                .id(id)
                .code("NV-" + id)
                .username(id)
                .usernameNormalized(id)
                .emailNormalized(id + "@x.vn")
                .fullName("Tên " + id)
                .role(role)
                .status(EmployeeStatus.ACTIVE)
                .securityVersion(1L)
                .build());
        return new Cookie("PMS_TOKEN", jwtService.issue(employee, Duration.ofMinutes(10)));
    }

    private Category category(String id, String code, String name) {
        return Category.builder()
                .id(id)
                .code(code)
                .name(name)
                .color("#ef4444")
                .status(CategoryStatus.ACTIVE)
                .build();
    }

    private Medicine medicine(
            String id,
            String code,
            String name,
            String categoryId,
            Long price,
            Long cost,
            PrescriptionType type,
            MedicineStatus status,
            int minStock) {
        return Medicine.builder()
                .id(id)
                .code(code)
                .name(name)
                .genericName(name)
                .manufacturer("Dược Hậu Giang")
                .unit("Hộp")
                .categoryId(categoryId)
                .price(price)
                .costPrice(cost)
                .prescriptionType(type)
                .status(status)
                .minStock(minStock)
                .createdAt(Instant.now())
                .build();
    }

    private StockBatch batch(
            String id,
            String medicineId,
            String code,
            LocalDate expiry,
            int initial,
            int remaining,
            BatchHandlingStatus handling,
            int line) {
        return StockBatch.builder()
                .id(id)
                .code(code)
                .medicineId(medicineId)
                .receiptId("r1")
                .receiptLineNo(line)
                .supplierId("sup-1")
                .expiryDate(expiry)
                .receivedDate(TODAY.minusDays(30))
                .initialQty(initial)
                .remainingQty(remaining)
                .purchasePrice(12000L)
                .handlingStatus(handling)
                .ledgerVersion(0L)
                .build();
    }

    private ResultActions as(Role role, String path) throws Exception {
        return mockMvc.perform(get(path).cookie(cookies.get(role)));
    }

    @Test
    void categoriesIncludeMedicineCounts() throws Exception {
        as(Role.SALES, "/api/v1/categories")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].code").value("GDHS"))
                .andExpect(jsonPath("$[0].medicineCount").value(2))
                .andExpect(jsonPath("$[1].medicineCount").value(1));
    }

    @Test
    void adminSeesCostPriceAndComputedStockFigures() throws Exception {
        as(Role.ADMIN, "/api/v1/medicines?q=paracetamol")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].costPrice").value(12000))
                .andExpect(jsonPath("$.items[0].categoryName").value("Giảm đau - Hạ sốt"))
                .andExpect(jsonPath("$.items[0].totalQty").value(85))
                .andExpect(jsonPath("$.items[0].availableQty").value(50))
                .andExpect(jsonPath("$.items[0].expiredQty").value(5))
                .andExpect(jsonPath("$.items[0].batchCount").value(3))
                .andExpect(jsonPath("$.items[0].nearestExpiry")
                        .value(TODAY.plusDays(200).toString()))
                .andExpect(jsonPath("$.items[0].stockStatus").value("IN_STOCK"));
    }

    @Test
    void salesSeesOnlySellableMedicinesWithoutCostPrice() throws Exception {
        as(Role.SALES, "/api/v1/medicines")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.items[*].costPrice").isEmpty())
                .andExpect(jsonPath("$.items[?(@.status != 'ACTIVE')]").isEmpty());
    }

    @Test
    void searchIgnoresDiacriticsAndCase() throws Exception {
        as(Role.ADMIN, "/api/v1/medicines?q=DUOC HAU")
                .andExpect(jsonPath("$.total").value(3));
        as(Role.ADMIN, "/api/v1/medicines?q=amox")
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].code").value("T0002"));
    }

    @Test
    void filtersCombineStockStatusPrescriptionAndCategory() throws Exception {
        as(Role.ADMIN, "/api/v1/medicines?stockStatus=LOW_STOCK")
                .andExpect(jsonPath("$.items[*].code", contains("T0002")));
        as(Role.ADMIN, "/api/v1/medicines?prescriptionType=RX")
                .andExpect(jsonPath("$.total").value(1));
        as(Role.ADMIN, "/api/v1/medicines?categoryId=cat-1&status=INACTIVE")
                .andExpect(jsonPath("$.items[*].code", contains("T0003")));
        as(Role.ADMIN, "/api/v1/medicines?stockStatus=OUT_OF_STOCK")
                .andExpect(jsonPath("$.items[*].code", contains("T0003")));
        as(Role.ADMIN, "/api/v1/medicines?inStock=true&sort=code,asc")
                .andExpect(jsonPath("$.items[*].code", contains("T0001", "T0002")));
    }

    @Test
    void sortsAndPaginatesWithOneBasedPagesAndClampsOutOfRangePages() throws Exception {
        as(Role.ADMIN, "/api/v1/medicines?sort=price,desc")
                .andExpect(jsonPath("$.items[*].code", contains("T0002", "T0003", "T0001")));
        as(Role.ADMIN, "/api/v1/medicines?sort=name,asc&size=2&page=2")
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.items", hasSize(1)));
        as(Role.ADMIN, "/api/v1/medicines?size=2&page=9")
                .andExpect(jsonPath("$.page").value(2));
        as(Role.ADMIN, "/api/v1/medicines?sort=hacker,desc").andExpect(status().isOk());
    }

    @Test
    void invalidEnumParameterIsABadRequest() throws Exception {
        as(Role.ADMIN, "/api/v1/medicines?status=BOGUS")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void adminDetailListsAllBatchesByExpiryWithMovementsAndCosts() throws Exception {
        as(Role.ADMIN, "/api/v1/medicines/m1")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.costPrice").value(12000))
                .andExpect(jsonPath("$.batches[*].code", contains("LOT2", "LOT1", "LOT3")))
                .andExpect(jsonPath("$.batches[0].status").value("EXPIRED"))
                .andExpect(jsonPath("$.batches[1].status").value("AVAILABLE"))
                .andExpect(jsonPath("$.batches[2].status").value("QUARANTINED"))
                .andExpect(jsonPath("$.batches[1].purchasePrice").value(12000))
                .andExpect(jsonPath("$.movements", hasSize(1)));
    }

    @Test
    void salesDetailHidesCostsMovementsAndEmptyBatches() throws Exception {
        as(Role.SALES, "/api/v1/medicines/m1")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.costPrice").doesNotExist())
                .andExpect(jsonPath("$.batches[0].purchasePrice").doesNotExist())
                .andExpect(jsonPath("$.movements", hasSize(0)));
        as(Role.SALES, "/api/v1/medicines/m3").andExpect(jsonPath("$.batches", hasSize(0)));
    }

    @Test
    void unknownMedicineIsNotFound() throws Exception {
        as(Role.ADMIN, "/api/v1/medicines/nope")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void notificationsDependOnTheRole() throws Exception {
        as(Role.ADMIN, "/api/v1/notifications")
                .andExpect(jsonPath("$[*].id", contains("low-stock", "expiring", "expired", "receipts", "stocktakes")))
                .andExpect(jsonPath("$[0].title").value("1 thuốc sắp hết hoặc đã hết"))
                .andExpect(jsonPath("$[0].link").value("pages/admin/inventory.html"));
        as(Role.WAREHOUSE, "/api/v1/notifications")
                .andExpect(jsonPath("$[*].id", contains("low-stock", "expiring", "expired", "receipts")))
                .andExpect(jsonPath("$[1].link").value("pages/warehouse/expiry.html"));
        as(Role.SALES, "/api/v1/notifications")
                .andExpect(jsonPath("$[*].id", contains("low-stock", "expiring", "expired")))
                .andExpect(jsonPath("$[0].link").value("pages/sales/lookup.html"));
    }

    @Test
    void settingsAreCreatedWithDefaultsOnFirstRead() throws Exception {
        as(Role.SALES, "/api/v1/settings")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pharmacy.name").value("Nhà thuốc Minh Tâm"))
                .andExpect(jsonPath("$.expiryWarningDays").value(90))
                .andExpect(jsonPath("$.expiryTiers", contains(30, 60, 90)))
                .andExpect(jsonPath("$.lowStockMultiplier").value(1.0))
                .andExpect(jsonPath("$.appearance.theme").value("light"))
                .andExpect(jsonPath("$.notifications.lowStock").value(true))
                .andExpect(jsonPath("$.notifications.dailyReport").value(false));
    }

    @Test
    void everySharedEndpointRequiresAuthentication() throws Exception {
        for (String path : new String[] {
            "/api/v1/settings",
            "/api/v1/notifications",
            "/api/v1/categories",
            "/api/v1/medicines",
            "/api/v1/medicines/m1"
        }) {
            mockMvc.perform(get(path)).andExpect(status().isUnauthorized());
        }
    }
}
