package com.pharmacy.demo.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.pharmacy.demo.entity.Category;
import com.pharmacy.demo.entity.Employee;
import com.pharmacy.demo.entity.Medicine;
import com.pharmacy.demo.entity.StockBatch;
import com.pharmacy.demo.entity.Supplier;
import com.pharmacy.demo.enums.BatchHandlingStatus;
import com.pharmacy.demo.enums.CategoryStatus;
import com.pharmacy.demo.enums.EmployeeStatus;
import com.pharmacy.demo.enums.MedicineStatus;
import com.pharmacy.demo.enums.MovementType;
import com.pharmacy.demo.enums.PrescriptionType;
import com.pharmacy.demo.enums.Role;
import com.pharmacy.demo.enums.SupplierStatus;
import com.pharmacy.demo.repository.CategoryRepository;
import com.pharmacy.demo.repository.DisposalRepository;
import com.pharmacy.demo.repository.EmployeeRepository;
import com.pharmacy.demo.repository.GoodsReceiptRepository;
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
class WarehouseApiIT {

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
    private JwtService jwtService;

    @BeforeEach
    void seed() {
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
        categoryRepository.save(category("cat-1", "DM1", "Giảm đau"));
        categoryRepository.save(category("cat-2", "DM2", "Vitamin"));
        supplierRepository.save(supplier("sup-1", "NCC001", "Dược Hậu Giang", SupplierStatus.ACTIVE));
        supplierRepository.save(supplier("sup-2", "NCC002", "Nhà cung cấp cũ", SupplierStatus.INACTIVE));
        medicineRepository.save(medicine("med-1", "T0001", "Paracetamol", "cat-1"));
        medicineRepository.save(medicine("med-2", "T0002", "Vitamin C", "cat-2"));
        batchRepository.save(batch("b-1", "L001", "med-1", 100, 400));
        batchRepository.save(batch("b-2", "L002", "med-1", 50, 200));
        batchRepository.save(batch("b-3", "L003", "med-2", 30, 300));
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

    private Category category(String id, String code, String name) {
        return Category.builder()
                .id(id)
                .code(code)
                .name(name)
                .status(CategoryStatus.ACTIVE)
                .createdAt(Instant.now())
                .build();
    }

    private Supplier supplier(String id, String code, String name, SupplierStatus status) {
        return Supplier.builder()
                .id(id)
                .code(code)
                .name(name)
                .status(status)
                .createdAt(Instant.now())
                .build();
    }

    private Medicine medicine(String id, String code, String name, String categoryId) {
        return Medicine.builder()
                .id(id)
                .code(code)
                .name(name)
                .unit("Viên")
                .categoryId(categoryId)
                .price(2000L)
                .costPrice(1000L)
                .minStock(10)
                .prescriptionType(PrescriptionType.OTC)
                .status(MedicineStatus.ACTIVE)
                .createdAt(Instant.now())
                .build();
    }

    private StockBatch batch(String id, String code, String medicineId, int quantity, int expiresInDays) {
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
                .handlingStatus(BatchHandlingStatus.AVAILABLE)
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

    private String idOf(ResultActions result) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id");
    }

    private int remaining(String batchId) {
        return batchRepository.findById(batchId).orElseThrow().getRemainingQty();
    }

    private BatchHandlingStatus handling(String batchId) {
        return batchRepository.findById(batchId).orElseThrow().getHandlingStatus();
    }

    private String receiptBody(String supplierId, boolean confirm, String itemJson) {
        return "{\"supplierId\":\"" + supplierId + "\",\"receivingDate\":\"" + LocalDate.now() + "\",\"confirm\":"
                + confirm + ",\"items\":[" + itemJson + "]}";
    }

    private String receiptItem(String medicineId, String batchCode, int expiresInDays, int quantity, long price) {
        return "{\"medicineId\":\"" + medicineId + "\",\"batchCode\":\"" + batchCode + "\",\"manufactureDate\":\""
                + LocalDate.now().minusDays(10) + "\",\"expiryDate\":\""
                + LocalDate.now().plusDays(expiresInDays)
                + "\",\"quantity\":" + quantity + ",\"purchasePrice\":" + price + "}";
    }

    @Test
    void confirmingAReceiptCreatesBatchesMovementsAndUpdatesCostPrice() throws Exception {
        String id = idOf(send(
                        post("/api/v1/receipts"),
                        "emp-kho",
                        receiptBody("sup-1", true, receiptItem("med-1", "lo-new", 365, 40, 1500)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.totalQuantity").value(40))
                .andExpect(jsonPath("$.totalAmount").value(60000))
                .andExpect(jsonPath("$.items[0].batchCode").value("LO-NEW"))
                .andExpect(jsonPath("$.code").value(org.hamcrest.Matchers.startsWith("PN-"))));

        StockBatch created = batchRepository.findAll().stream()
                .filter(batch -> "LO-NEW".equals(batch.getCode()))
                .findFirst()
                .orElseThrow();
        assertThat(created.getRemainingQty()).isEqualTo(40);
        assertThat(created.getReceiptId()).isEqualTo(id);
        assertThat(movementRepository.findAll())
                .filteredOn(movement -> movement.getType() == MovementType.IN)
                .hasSize(1);
        assertThat(medicineRepository.findById("med-1").orElseThrow().getCostPrice())
                .isEqualTo(1500L);
    }

    @Test
    void draftsAreEditableAndConfirmedReceiptsAreFrozen() throws Exception {
        String draftItem = "{\"medicineId\":\"med-1\",\"quantity\":10,\"purchasePrice\":900}";
        String id = idOf(send(post("/api/v1/receipts"), "emp-kho", receiptBody("sup-1", false, draftItem))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT")));
        assertThat(batchRepository.count()).isEqualTo(3);

        send(
                        put("/api/v1/receipts/" + id),
                        "emp-kho",
                        receiptBody("sup-1", true, receiptItem("med-1", "LO-D1", 300, 12, 900)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        send(
                        put("/api/v1/receipts/" + id),
                        "emp-kho",
                        receiptBody("sup-1", true, receiptItem("med-1", "LO-D2", 300, 12, 900)))
                .andExpect(status().isConflict());
        send(delete("/api/v1/receipts/" + id), "emp-kho", null).andExpect(status().isConflict());
        assertThat(batchRepository.count()).isEqualTo(4);
        assertThat(movementRepository.count()).isEqualTo(1);
    }

    @Test
    void draftReceiptsCanBeDeleted() throws Exception {
        String id = idOf(send(
                        post("/api/v1/receipts"),
                        "emp-kho",
                        receiptBody("sup-1", false, "{\"medicineId\":\"med-1\",\"quantity\":5,\"purchasePrice\":0}"))
                .andExpect(status().isOk()));

        send(delete("/api/v1/receipts/" + id), "emp-kho", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertThat(receiptRepository.count()).isZero();
    }

    @Test
    void confirmationRejectsInvalidLinesWithLineLevelErrors() throws Exception {
        String items = receiptItem("med-1", "L001", 300, 10, 100) + ","
                + "{\"medicineId\":\"med-2\",\"batchCode\":\"\",\"quantity\":0,\"purchasePrice\":-1,"
                + "\"expiryDate\":\"" + LocalDate.now().minusDays(1) + "\"}";

        send(post("/api/v1/receipts"), "emp-kho", receiptBody("sup-1", true, items))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors['items.0.batchCode']").exists())
                .andExpect(jsonPath("$.fieldErrors['items.1.batchCode']").exists())
                .andExpect(jsonPath("$.fieldErrors['items.1.quantity']").exists())
                .andExpect(jsonPath("$.fieldErrors['items.1.purchasePrice']").exists())
                .andExpect(jsonPath("$.fieldErrors['items.1.expiryDate']").exists());

        assertThat(receiptRepository.count()).isZero();
        assertThat(batchRepository.count()).isEqualTo(3);
    }

    @Test
    void duplicateBatchCodesInsideOneReceiptAreRejected() throws Exception {
        String items = receiptItem("med-1", "DUP", 300, 5, 100) + "," + receiptItem("med-1", "dup", 300, 5, 100);

        send(post("/api/v1/receipts"), "emp-kho", receiptBody("sup-1", true, items))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors['items.1.batchCode']").exists());
    }

    @Test
    void inactiveSuppliersCannotConfirmButCanHoldDrafts() throws Exception {
        send(post("/api/v1/receipts"), "emp-kho", receiptBody("sup-2", true, receiptItem("med-1", "LX", 300, 5, 100)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.supplierId").exists());
        send(
                        post("/api/v1/receipts"),
                        "emp-kho",
                        receiptBody("sup-2", false, "{\"medicineId\":\"med-1\",\"quantity\":5,\"purchasePrice\":1}"))
                .andExpect(status().isOk());
    }

    @Test
    void onlyWarehouseStaffCanWriteReceipts() throws Exception {
        String body = receiptBody("sup-1", true, receiptItem("med-1", "LA", 300, 5, 100));
        send(post("/api/v1/receipts"), "emp-admin", body).andExpect(status().isForbidden());
        send(post("/api/v1/receipts"), "emp-sales", body).andExpect(status().isForbidden());
        send(get("/api/v1/receipts/next-code"), "emp-admin", null).andExpect(status().isForbidden());
        send(get("/api/v1/receipts/next-code"), "emp-kho", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(org.hamcrest.Matchers.startsWith("PN-")));
    }

    @Test
    void stocktakeSnapshotsBatchesInScopeAndFollowsTheDraftToApprovalFlow() throws Exception {
        String id = idOf(send(post("/api/v1/stocktakes"), "emp-kho", "{\"categoryId\":\"cat-1\",\"note\":\"Quý IV\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.scopeLabel").value("Giảm đau"))
                .andExpect(jsonPath("$.lines.length()").value(2))
                .andExpect(jsonPath("$.lines[0].actualQty").doesNotExist()));

        send(
                        post("/api/v1/stocktakes/" + id + "/submit"),
                        "emp-kho",
                        "{\"lines\":[{\"batchId\":\"b-1\",\"actualQty\":100}]}")
                .andExpect(status().isBadRequest());

        send(
                        put("/api/v1/stocktakes/" + id),
                        "emp-kho",
                        "{\"lines\":[{\"batchId\":\"b-1\",\"actualQty\":98,\"reason\":\"Hao hụt\"}]}")
                .andExpect(status().isOk());

        send(
                        post("/api/v1/stocktakes/" + id + "/submit"),
                        "emp-kho",
                        "{\"lines\":[{\"batchId\":\"b-2\",\"actualQty\":50}]}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"));

        send(put("/api/v1/stocktakes/" + id), "emp-kho", "{\"lines\":[]}").andExpect(status().isConflict());
        send(delete("/api/v1/stocktakes/" + id), "emp-kho", null).andExpect(status().isConflict());

        send(post("/api/v1/stocktakes/" + id + "/approve"), "emp-admin", "{}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
        assertThat(remaining("b-1")).isEqualTo(98);
        assertThat(remaining("b-3")).isEqualTo(30);
    }

    @Test
    void wholeWarehouseStocktakeIncludesEveryBatchAndDraftsCanBeDeleted() throws Exception {
        String id = idOf(send(post("/api/v1/stocktakes"), "emp-kho", "{\"categoryId\":\"ALL\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scope").value("ALL"))
                .andExpect(jsonPath("$.lines.length()").value(3)));

        send(delete("/api/v1/stocktakes/" + id), "emp-kho", null).andExpect(status().isOk());

        assertThat(stocktakeRepository.count()).isZero();
        send(post("/api/v1/stocktakes"), "emp-admin", "{\"categoryId\":\"ALL\"}")
                .andExpect(status().isForbidden());
    }

    private String disposalBody(String batchId, int quantity) {
        return "{\"reason\":\"Thuốc hết hạn\",\"lines\":[{\"batchId\":\"" + batchId + "\",\"quantity\":" + quantity
                + ",\"reasonType\":\"EXPIRED\"}]}";
    }

    @Test
    void disposalRunsProposeApproveExecuteAndOnlyThenRemovesStock() throws Exception {
        String id = idOf(send(post("/api/v1/disposals"), "emp-kho", disposalBody("b-2", 20))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT")));
        assertThat(handling("b-2")).isEqualTo(BatchHandlingStatus.AVAILABLE);

        send(post("/api/v1/disposals/" + id + "/submit"), "emp-kho", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"));
        assertThat(handling("b-2")).isEqualTo(BatchHandlingStatus.QUARANTINED);
        assertThat(remaining("b-2")).isEqualTo(50);

        send(post("/api/v1/disposals/" + id + "/execute"), "emp-kho", null).andExpect(status().isConflict());
        assertThat(remaining("b-2")).isEqualTo(50);

        send(post("/api/v1/disposals/" + id + "/approve"), "emp-admin", "{}").andExpect(status().isOk());
        assertThat(remaining("b-2")).isEqualTo(50);

        send(post("/api/v1/disposals/" + id + "/execute"), "emp-kho", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXECUTED"))
                .andExpect(jsonPath("$.executedByName").value("Người dùng kho"));
        assertThat(remaining("b-2")).isEqualTo(30);
        assertThat(handling("b-2")).isEqualTo(BatchHandlingStatus.AVAILABLE);
        assertThat(movementRepository.findAll())
                .filteredOn(movement -> movement.getType() == MovementType.WRITE_OFF)
                .singleElement()
                .satisfies(movement -> assertThat(movement.getQuantity()).isEqualTo(-20));

        send(post("/api/v1/disposals/" + id + "/execute"), "emp-kho", null).andExpect(status().isConflict());
        assertThat(remaining("b-2")).isEqualTo(30);
    }

    @Test
    void disposingTheWholeBatchMarksItDisposed() throws Exception {
        String id = idOf(send(post("/api/v1/disposals"), "emp-kho", disposalBody("b-3", 30))
                .andExpect(status().isOk()));
        send(post("/api/v1/disposals/" + id + "/submit"), "emp-kho", null).andExpect(status().isOk());
        send(post("/api/v1/disposals/" + id + "/approve"), "emp-admin", "{}").andExpect(status().isOk());
        send(post("/api/v1/disposals/" + id + "/execute"), "emp-kho", null).andExpect(status().isOk());

        assertThat(remaining("b-3")).isZero();
        assertThat(handling("b-3")).isEqualTo(BatchHandlingStatus.DISPOSED);
    }

    @Test
    void rejectedDisposalReleasesTheBatch() throws Exception {
        String id = idOf(send(post("/api/v1/disposals"), "emp-kho", disposalBody("b-1", 10))
                .andExpect(status().isOk()));
        send(post("/api/v1/disposals/" + id + "/submit"), "emp-kho", null).andExpect(status().isOk());
        assertThat(handling("b-1")).isEqualTo(BatchHandlingStatus.QUARANTINED);

        send(post("/api/v1/disposals/" + id + "/reject"), "emp-admin", "{\"note\":\"Chưa đủ căn cứ\"}")
                .andExpect(status().isOk());

        assertThat(handling("b-1")).isEqualTo(BatchHandlingStatus.AVAILABLE);
        assertThat(remaining("b-1")).isEqualTo(100);
        send(post("/api/v1/disposals/" + id + "/execute"), "emp-kho", null).andExpect(status().isConflict());
    }

    @Test
    void disposalValidatesQuantityAndRejectsBatchesInAnotherOpenDisposal() throws Exception {
        send(post("/api/v1/disposals"), "emp-kho", disposalBody("b-3", 31)).andExpect(status().isBadRequest());
        send(post("/api/v1/disposals"), "emp-kho", disposalBody("missing", 1)).andExpect(status().isBadRequest());
        send(post("/api/v1/disposals"), "emp-kho", disposalBody("b-3", 5)).andExpect(status().isOk());

        send(post("/api/v1/disposals"), "emp-kho", disposalBody("b-3", 5)).andExpect(status().isConflict());
        assertThat(disposalRepository.count()).isEqualTo(1);
    }

    @Test
    void draftDisposalsCanBeEditedAndDeletedButSubmittedOnesCannot() throws Exception {
        String id = idOf(send(post("/api/v1/disposals"), "emp-kho", disposalBody("b-1", 10))
                .andExpect(status().isOk()));

        send(put("/api/v1/disposals/" + id), "emp-kho", disposalBody("b-1", 15))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lines[0].quantity").value(15));

        send(post("/api/v1/disposals/" + id + "/submit"), "emp-kho", null).andExpect(status().isOk());

        send(put("/api/v1/disposals/" + id), "emp-kho", disposalBody("b-1", 5)).andExpect(status().isConflict());
        send(delete("/api/v1/disposals/" + id), "emp-kho", null).andExpect(status().isConflict());
        send(post("/api/v1/disposals/" + id + "/submit"), "emp-kho", null).andExpect(status().isConflict());
    }

    @Test
    void onlyWarehouseStaffProposeAndExecuteDisposals() throws Exception {
        send(post("/api/v1/disposals"), "emp-admin", disposalBody("b-1", 1)).andExpect(status().isForbidden());
        send(post("/api/v1/disposals"), "emp-sales", disposalBody("b-1", 1)).andExpect(status().isForbidden());
        String id = idOf(send(post("/api/v1/disposals"), "emp-kho", disposalBody("b-1", 1))
                .andExpect(status().isOk()));
        send(post("/api/v1/disposals/" + id + "/execute"), "emp-admin", null).andExpect(status().isForbidden());
        send(post("/api/v1/disposals/" + id + "/approve"), "emp-kho", "{}").andExpect(status().isForbidden());
    }

    @Test
    void warehouseDashboardAndExpirySummaryReflectTheStock() throws Exception {
        send(post("/api/v1/receipts"), "emp-kho", receiptBody("sup-1", true, receiptItem("med-2", "LD", 20, 8, 100)))
                .andExpect(status().isOk());
        send(
                        post("/api/v1/receipts"),
                        "emp-kho",
                        receiptBody("sup-1", false, "{\"medicineId\":\"med-1\",\"quantity\":1,\"purchasePrice\":1}"))
                .andExpect(status().isOk());

        send(get("/api/v1/dashboard/warehouse"), "emp-kho", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kpis.totalBatches").value(4))
                .andExpect(jsonPath("$.kpis.pendingReceipts").value(1))
                .andExpect(jsonPath("$.kpis.movementsToday").value(1))
                .andExpect(jsonPath("$.tasks.length()").value(6))
                .andExpect(jsonPath("$.movementSeries.length()").value(14))
                .andExpect(jsonPath("$.movementSeries[13].inQty").value(8))
                .andExpect(jsonPath("$.recentReceipts.length()").value(2))
                .andExpect(jsonPath("$.recentMovements.length()").value(1));
        send(get("/api/v1/dashboard/warehouse"), "emp-sales", null).andExpect(status().isForbidden());

        send(get("/api/v1/expiry/summary"), "emp-kho", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tiers.length()").value(3))
                .andExpect(jsonPath("$.expired").value(0));
        send(get("/api/v1/expiry/summary"), "emp-sales", null).andExpect(status().isForbidden());
    }
}
