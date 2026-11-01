package com.pharmacy.demo.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pharmacy.demo.entity.Customer;
import com.pharmacy.demo.entity.Employee;
import com.pharmacy.demo.entity.Invoice;
import com.pharmacy.demo.entity.Medicine;
import com.pharmacy.demo.enums.EmployeeStatus;
import com.pharmacy.demo.enums.InvoiceStatus;
import com.pharmacy.demo.enums.MedicineStatus;
import com.pharmacy.demo.enums.PrescriptionType;
import com.pharmacy.demo.enums.Role;
import com.pharmacy.demo.repository.CustomerRepository;
import com.pharmacy.demo.repository.EmployeeRepository;
import com.pharmacy.demo.repository.InvoiceRepository;
import com.pharmacy.demo.repository.MedicineRepository;
import com.pharmacy.demo.repository.SystemSettingRepository;
import com.pharmacy.demo.security.JwtService;
import com.pharmacy.demo.support.MongoIntegrationTest;
import jakarta.servlet.http.Cookie;
import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@MongoIntegrationTest
@AutoConfigureMockMvc
class AccountTierImageApiIT {

    private static final String OLD_PASSWORD = "OldPass123";
    private static final byte[] PNG = {
        (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D, 'I', 'H', 'D', 'R'
    };

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private MedicineRepository medicineRepository;

    @Autowired
    private SystemSettingRepository settingRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @BeforeEach
    void seed() {
        invoiceRepository.deleteAll();
        customerRepository.deleteAll();
        medicineRepository.deleteAll();
        employeeRepository.deleteAll();
        settingRepository.deleteAll();
        employeeRepository.save(employee("emp-admin", "admin", Role.ADMIN));
        employeeRepository.save(employee("emp-sales", "sales", Role.SALES));
        customerRepository.save(customer("cus-1", "KH0001", "0901000001"));
        customerRepository.save(customer("cus-2", "KH0002", "0901000002"));
        invoiceRepository.save(invoice("inv-1", "cus-1", 5_500_000L, InvoiceStatus.COMPLETED));
        invoiceRepository.save(invoice("inv-2", "cus-2", 900_000L, InvoiceStatus.COMPLETED));
        invoiceRepository.save(invoice("inv-3", "cus-2", 4_000_000L, InvoiceStatus.CANCELLED));
        medicineRepository.save(Medicine.builder()
                .id("med-bulk-1")
                .code("T0001")
                .name("Paracetamol")
                .unit("Hộp")
                .categoryId("cat-1")
                .price(50000L)
                .minStock(5)
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
                .email(username + "@pharmacy.demo")
                .emailNormalized(username + "@pharmacy.demo")
                .passwordHash(passwordEncoder.encode(OLD_PASSWORD))
                .role(role)
                .status(EmployeeStatus.ACTIVE)
                .securityVersion(1L)
                .createdAt(Instant.now())
                .build();
    }

    private Customer customer(String id, String code, String phone) {
        return Customer.builder()
                .id(id)
                .code(code)
                .name("Khách " + code)
                .phone(phone)
                .createdAt(Instant.now())
                .build();
    }

    private Invoice invoice(String id, String customerId, long total, InvoiceStatus status) {
        return Invoice.builder()
                .id(id)
                .code("HD-" + id)
                .customerId(customerId)
                .total(total)
                .status(status)
                .createdAt(Instant.now())
                .build();
    }

    private Cookie cookie(String actorId) {
        Employee employee = employeeRepository.findById(actorId).orElseThrow();
        return new Cookie("PMS_TOKEN", jwtService.issue(employee, Duration.ofMinutes(10)));
    }

    private ResultActions send(MockHttpServletRequestBuilder builder, String actorId, String body) throws Exception {
        MockHttpServletRequestBuilder request = builder.cookie(cookie(actorId));
        if (body != null) {
            request = request.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return mockMvc.perform(request);
    }

    private String changeBody(String current, String next) {
        return "{\"currentPassword\":\"" + current + "\",\"newPassword\":\"" + next + "\"}";
    }

    @Test
    void changingPasswordRejectsWrongCurrentPasswordAndWeakNewPassword() throws Exception {
        send(post("/api/v1/auth/change-password"), "emp-sales", changeBody("Wrong12345", "NewPass456"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.currentPassword").exists());
        send(post("/api/v1/auth/change-password"), "emp-sales", changeBody(OLD_PASSWORD, "weak"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.newPassword").exists());
        send(post("/api/v1/auth/change-password"), "emp-sales", changeBody(OLD_PASSWORD, OLD_PASSWORD))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.newPassword").exists());
    }

    @Test
    void changingPasswordIssuesNewSessionAndRevokesTheOldOne() throws Exception {
        Cookie oldCookie = cookie("emp-sales");
        mockMvc.perform(post("/api/v1/auth/change-password")
                        .cookie(oldCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(changeBody(OLD_PASSWORD, "NewPass456")))
                .andExpect(status().isOk())
                .andExpect(header().exists("Set-Cookie"));

        mockMvc.perform(get("/api/v1/auth/me").cookie(oldCookie)).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"sales\",\"password\":\"NewPass456\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void tierFollowsCompletedSpendingAndIgnoresCancelledInvoices() throws Exception {
        send(get("/api/v1/customers/cus-1"), "emp-sales", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tier").value("GOLD"))
                .andExpect(jsonPath("$.nextTier").value("DIAMOND"))
                .andExpect(jsonPath("$.amountToNextTier").value(4_500_000))
                .andExpect(jsonPath("$.earnRatePercent").value(150));
        send(get("/api/v1/customers/cus-2"), "emp-sales", null)
                .andExpect(jsonPath("$.tier").value("STANDARD"))
                .andExpect(jsonPath("$.nextTier").value("SILVER"))
                .andExpect(jsonPath("$.amountToNextTier").value(1_100_000));
    }

    @Test
    void customerListCanBeFilteredByTierAndSummaryCountsEachTier() throws Exception {
        send(get("/api/v1/customers").param("tier", "GOLD"), "emp-sales", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].code").value("KH0001"));
        send(get("/api/v1/customers/loyalty-summary"), "emp-sales", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMembers").value(2))
                .andExpect(jsonPath("$.tiers[0].customerCount").value(1))
                .andExpect(jsonPath("$.tiers[2].customerCount").value(1));
    }

    @Test
    void loyaltyThresholdsMustIncrease() throws Exception {
        send(put("/api/v1/settings"), "emp-admin", "{\"loyalty\":{\"silverThreshold\":6000000}}")
                .andExpect(status().isBadRequest());
        send(put("/api/v1/settings"), "emp-admin", "{\"loyalty\":{\"silverThreshold\":1000000,\"goldRate\":160}}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loyalty.silverThreshold").value(1_000_000))
                .andExpect(jsonPath("$.loyalty.goldRate").value(160));
    }

    @Test
    void bulkUploadMatchesFileNamesToMedicineCodesInsideAZip() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            zip.putNextEntry(new ZipEntry("anh/t0001.png"));
            zip.write(PNG);
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("anh/unknown.png"));
            zip.write(PNG);
            zip.closeEntry();
        }
        MockMultipartFile archive =
                new MockMultipartFile("files", "images.zip", "application/zip", bytes.toByteArray());

        mockMvc.perform(multipart("/api/v1/medicines/images/bulk").file(archive).cookie(cookie("emp-admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saved").value(1))
                .andExpect(jsonPath("$.noMatch").value(1))
                .andExpect(
                        jsonPath("$.items[?(@.medicineCode=='T0001')].status").value("SAVED"));
        assertThat(medicineRepository.findById("med-bulk-1").orElseThrow().getImageUrl())
                .isNotNull();

        MockMultipartFile again = new MockMultipartFile("files", "T0001.png", "image/png", PNG);
        mockMvc.perform(multipart("/api/v1/medicines/images/bulk").file(again).cookie(cookie("emp-admin")))
                .andExpect(jsonPath("$.saved").value(0))
                .andExpect(jsonPath("$.skipped").value(1));
        mockMvc.perform(multipart("/api/v1/medicines/images/bulk")
                        .file(again)
                        .param("overwrite", "true")
                        .cookie(cookie("emp-admin")))
                .andExpect(jsonPath("$.saved").value(1));
        mockMvc.perform(multipart("/api/v1/medicines/images/bulk").file(again).cookie(cookie("emp-sales")))
                .andExpect(status().isForbidden());
    }
}
