package com.pharmacy.demo.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pharmacy.demo.entity.Customer;
import com.pharmacy.demo.entity.Employee;
import com.pharmacy.demo.entity.Medicine;
import com.pharmacy.demo.entity.StockBatch;
import com.pharmacy.demo.enums.BatchHandlingStatus;
import com.pharmacy.demo.enums.EmployeeStatus;
import com.pharmacy.demo.enums.MedicineStatus;
import com.pharmacy.demo.enums.PrescriptionType;
import com.pharmacy.demo.enums.Role;
import com.pharmacy.demo.repository.CustomerRepository;
import com.pharmacy.demo.repository.EmployeeRepository;
import com.pharmacy.demo.repository.InvoiceRepository;
import com.pharmacy.demo.repository.LoyaltyTransactionRepository;
import com.pharmacy.demo.repository.MedicineRepository;
import com.pharmacy.demo.repository.StockBatchRepository;
import com.pharmacy.demo.repository.StockMovementRepository;
import com.pharmacy.demo.repository.SystemSettingRepository;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.ObjectMapper;

@MongoIntegrationTest
@AutoConfigureMockMvc
class LoyaltyApiIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
    private LoyaltyTransactionRepository loyaltyRepository;

    @Autowired
    private SystemSettingRepository settingRepository;

    @Autowired
    private JwtService jwtService;

    @BeforeEach
    void seed() {
        loyaltyRepository.deleteAll();
        invoiceRepository.deleteAll();
        movementRepository.deleteAll();
        batchRepository.deleteAll();
        medicineRepository.deleteAll();
        customerRepository.deleteAll();
        employeeRepository.deleteAll();
        settingRepository.deleteAll();
        employeeRepository.save(employee("emp-admin", "admin", Role.ADMIN));
        employeeRepository.save(employee("emp-sales", "sales", Role.SALES));
        customerRepository.save(customer("cus-1", "KH0001", "0901234567", 30));
        medicineRepository.save(Medicine.builder()
                .id("med-1")
                .code("T0001")
                .name("Paracetamol")
                .unit("Hộp")
                .categoryId("cat-1")
                .price(50000L)
                .costPrice(30000L)
                .minStock(5)
                .prescriptionType(PrescriptionType.OTC)
                .status(MedicineStatus.ACTIVE)
                .createdAt(Instant.now())
                .build());
        batchRepository.save(StockBatch.builder()
                .id("b-1")
                .code("L-1")
                .medicineId("med-1")
                .receiptId("rec-1")
                .receiptLineNo(1)
                .supplierId("sup-1")
                .receivedDate(LocalDate.now().minusDays(30))
                .expiryDate(LocalDate.now().plusDays(300))
                .initialQty(100)
                .remainingQty(100)
                .purchasePrice(30000L)
                .handlingStatus(BatchHandlingStatus.AVAILABLE)
                .ledgerVersion(0L)
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

    private Customer customer(String id, String code, String phone, int points) {
        return Customer.builder()
                .id(id)
                .code(code)
                .name("Nguyễn Văn A")
                .phone(phone)
                .loyaltyPoints(points)
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

    private String invoiceBody(int quantity, String customerId, int pointsUsed) {
        String customerPart = customerId == null ? "" : ",\"customerId\":\"" + customerId + "\"";
        return "{\"items\":[{\"medicineId\":\"med-1\",\"quantity\":" + quantity
                + "}],\"paymentMethod\":\"CASH\",\"receivedAmount\":10000000,\"pointsUsed\":" + pointsUsed
                + customerPart + "}";
    }

    private String createInvoice(String body) throws Exception {
        MvcResult result = send(post("/api/v1/invoices"), "emp-sales", body)
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper
                .readTree(result.getResponse().getContentAsString())
                .get("id")
                .stringValue();
    }

    private int points(String customerId) {
        return customerRepository.findById(customerId).orElseThrow().getLoyaltyPoints();
    }

    @Test
    void earnsOnePointPerTenThousandOfTheAmountActuallyPaid() throws Exception {
        send(post("/api/v1/invoices"), "emp-sales", invoiceBody(3, "cus-1", 0))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(150000))
                .andExpect(jsonPath("$.pointsEarned").value(15))
                .andExpect(jsonPath("$.pointsUsed").value(0));

        assertThat(points("cus-1")).isEqualTo(45);
        assertThat(loyaltyRepository.count()).isEqualTo(1);
    }

    @Test
    void walkInCustomersEarnNothing() throws Exception {
        send(post("/api/v1/invoices"), "emp-sales", invoiceBody(3, null, 0))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pointsEarned").value(0));

        assertThat(loyaltyRepository.count()).isZero();
    }

    @Test
    void redeemingPointsLowersTheTotalAndEarnsOnTheRemainder() throws Exception {
        send(post("/api/v1/invoices"), "emp-sales", invoiceBody(3, "cus-1", 20))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pointsUsed").value(20))
                .andExpect(jsonPath("$.pointsDiscount").value(20000))
                .andExpect(jsonPath("$.total").value(130000))
                .andExpect(jsonPath("$.pointsEarned").value(13));

        assertThat(points("cus-1")).isEqualTo(30 - 20 + 13);
        assertThat(loyaltyRepository.count()).isEqualTo(2);
    }

    @Test
    void rejectsRedeemingMoreThanTheBalanceTheCapOrWithoutACustomer() throws Exception {
        send(post("/api/v1/invoices"), "emp-sales", invoiceBody(3, "cus-1", 31))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.pointsUsed").exists());

        customerRepository.save(customer("cus-1", "KH0001", "0901234567", 500));
        send(post("/api/v1/invoices"), "emp-sales", invoiceBody(1, "cus-1", 26))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.pointsUsed").exists());
        send(post("/api/v1/invoices"), "emp-sales", invoiceBody(1, null, 5))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.pointsUsed").exists());

        assertThat(points("cus-1")).isEqualTo(500);
        assertThat(invoiceRepository.count()).isZero();
        assertThat(batchRepository.findById("b-1").orElseThrow().getRemainingQty())
                .isEqualTo(100);
    }

    @Test
    void acceptsRedeemingExactlyUpToFiftyPercent() throws Exception {
        customerRepository.save(customer("cus-1", "KH0001", "0901234567", 500));

        send(post("/api/v1/invoices"), "emp-sales", invoiceBody(1, "cus-1", 25))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(25000))
                .andExpect(jsonPath("$.pointsEarned").value(2));
    }

    @Test
    void cancellingRefundsUsedPointsAndRevokesEarnedOnes() throws Exception {
        String invoiceId = createInvoice(invoiceBody(3, "cus-1", 20));
        assertThat(points("cus-1")).isEqualTo(23);

        send(post("/api/v1/invoices/" + invoiceId + "/cancel"), "emp-admin", "{\"reason\":\"Khách trả hàng\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        assertThat(points("cus-1")).isEqualTo(30);
        send(get("/api/v1/customers/cus-1/loyalty"), "emp-admin", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[?(@.type=='EARN_REVOKE')]").exists())
                .andExpect(jsonPath("$[?(@.type=='REDEEM_REFUND')]").exists());
    }

    @Test
    void cancellingNeverDrivesTheBalanceBelowZero() throws Exception {
        customerRepository.save(customer("cus-1", "KH0001", "0901234567", 0));
        String invoiceId = createInvoice(invoiceBody(3, "cus-1", 0));
        assertThat(points("cus-1")).isEqualTo(15);
        customerRepository.save(customer("cus-1", "KH0001", "0901234567", 4));

        send(post("/api/v1/invoices/" + invoiceId + "/cancel"), "emp-admin", "{\"reason\":\"Nhập nhầm\"}")
                .andExpect(status().isOk());

        assertThat(points("cus-1")).isZero();
        send(get("/api/v1/customers/cus-1/loyalty"), "emp-admin", null)
                .andExpect(jsonPath("$[?(@.type=='EARN_REVOKE')].points").value(-4))
                .andExpect(jsonPath("$[?(@.type=='EARN_REVOKE')].note")
                        .value(org.hamcrest.Matchers.hasItem(org.hamcrest.Matchers.containsString("4/15"))));
    }

    @Test
    void disablingTheProgramStopsEarningAndRedeeming() throws Exception {
        send(put("/api/v1/settings"), "emp-admin", "{\"loyalty\":{\"enabled\":false}}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loyalty.enabled").value(false));

        send(post("/api/v1/invoices"), "emp-sales", invoiceBody(3, "cus-1", 0))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pointsEarned").value(0));
        send(post("/api/v1/invoices"), "emp-sales", invoiceBody(3, "cus-1", 5)).andExpect(status().isBadRequest());
    }

    @Test
    void settingsExposeAndValidateTheLoyaltyConfiguration() throws Exception {
        send(get("/api/v1/settings"), "emp-sales", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loyalty.enabled").value(true))
                .andExpect(jsonPath("$.loyalty.earnAmountPerPoint").value(10000))
                .andExpect(jsonPath("$.loyalty.pointValue").value(1000))
                .andExpect(jsonPath("$.loyalty.maxRedeemPercent").value(50));

        send(put("/api/v1/settings"), "emp-admin", "{\"loyalty\":{\"earnAmountPerPoint\":20000,\"pointValue\":500}}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loyalty.earnAmountPerPoint").value(20000));
        send(put("/api/v1/settings"), "emp-admin", "{\"loyalty\":{\"maxRedeemPercent\":150}}")
                .andExpect(status().isBadRequest());

        send(post("/api/v1/invoices"), "emp-sales", invoiceBody(4, "cus-1", 0))
                .andExpect(jsonPath("$.pointsEarned").value(10));
    }

    @Test
    void findsACustomerByPhoneIgnoringSpacesAndExposesThePoints() throws Exception {
        send(get("/api/v1/customers/by-phone?phone=0901 234 567"), "emp-sales", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("cus-1"))
                .andExpect(jsonPath("$.loyaltyPoints").value(30));
        send(get("/api/v1/customers/by-phone?phone=0999999999"), "emp-sales", null)
                .andExpect(status().isNotFound());
    }
}
