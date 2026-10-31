package com.pharmacy.demo.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.pharmacy.demo.entity.Employee;
import com.pharmacy.demo.entity.VerificationToken;
import com.pharmacy.demo.enums.EmployeeStatus;
import com.pharmacy.demo.enums.Role;
import com.pharmacy.demo.repository.EmployeeRepository;
import com.pharmacy.demo.repository.InvoiceRepository;
import com.pharmacy.demo.repository.VerificationTokenRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@MongoIntegrationTest
@AutoConfigureMockMvc
class EmployeeApiIT {

    private static final String PASSWORD = "Admin@12345";
    private static final String NEW_PASSWORD = "Fresh@12345";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private VerificationTokenRepository tokenRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @BeforeEach
    void seed() {
        invoiceRepository.deleteAll();
        tokenRepository.deleteAll();
        employeeRepository.deleteAll();
        employeeRepository.save(employee("emp-admin", "admin", Role.ADMIN, EmployeeStatus.ACTIVE));
        employeeRepository.save(employee("emp-sales", "sales", Role.SALES, EmployeeStatus.ACTIVE));
        employeeRepository.save(employee("emp-kho", "kho", Role.WAREHOUSE, EmployeeStatus.ACTIVE));
    }

    private Employee employee(String id, String username, Role role, EmployeeStatus status) {
        return Employee.builder()
                .id(id)
                .code("NV-" + id)
                .username(username)
                .usernameNormalized(username)
                .fullName("Người dùng " + username)
                .email(username + "@pharmacy.demo")
                .emailNormalized(username + "@pharmacy.demo")
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .role(role)
                .status(status)
                .securityVersion(1L)
                .createdAt(Instant.now())
                .build();
    }

    private Cookie cookieOf(String id) {
        Employee employee = employeeRepository.findById(id).orElseThrow();
        return new Cookie("PMS_TOKEN", jwtService.issue(employee, Duration.ofMinutes(10)));
    }

    private MvcResult createEmployee(String body) throws Exception {
        return mockMvc.perform(post("/api/v1/employees")
                        .cookie(cookieOf("emp-admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn();
    }

    private static String createBody(String username, String email) {
        return "{\"name\":\"Nguyễn Văn A\",\"email\":\"" + email + "\",\"username\":\"" + username
                + "\",\"phone\":\"0912345678\",\"role\":\"SALES\"}";
    }

    private static String tokenOf(MvcResult result, String path) throws Exception {
        String link = JsonPath.read(result.getResponse().getContentAsString(), path);
        return link.substring(link.indexOf("token=") + 6);
    }

    private int login(String identifier, String password) throws Exception {
        String body = "{\"identifier\":\"" + identifier + "\",\"password\":\"" + password + "\"}";
        return mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn()
                .getResponse()
                .getStatus();
    }

    private void patchJson(String path, String body, String actorId, int expectedStatus) throws Exception {
        mockMvc.perform(patch(path)
                        .cookie(cookieOf(actorId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is(expectedStatus));
    }

    @Test
    void adminListsFiltersAndReadsEmployees() throws Exception {
        mockMvc.perform(get("/api/v1/employees").cookie(cookieOf("emp-admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.items[0].name").exists())
                .andExpect(jsonPath("$.items[0].locked").value(false));
        mockMvc.perform(get("/api/v1/employees")
                        .param("role", "SALES")
                        .param("q", "SALES")
                        .cookie(cookieOf("emp-admin")))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].username").value("sales"));
        mockMvc.perform(get("/api/v1/employees/emp-sales").cookie(cookieOf("emp-admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invoiceCount").value(0))
                .andExpect(jsonPath("$.revenue").value(0))
                .andExpect(jsonPath("$.receiptCount").value(0))
                .andExpect(jsonPath("$.activities").isArray());
        mockMvc.perform(get("/api/v1/employees/missing").cookie(cookieOf("emp-admin")))
                .andExpect(status().isNotFound());
    }

    @Test
    void nonAdminsAndAnonymousCannotUseEmployeeEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/employees").cookie(cookieOf("emp-sales"))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/employees").cookie(cookieOf("emp-kho"))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/employees")).andExpect(status().isUnauthorized());
    }

    @Test
    void activationLinkLetsNewEmployeeSetPasswordExactlyOnce() throws Exception {
        MvcResult created = createEmployee(createBody("new.user", "new.user@pharmacy.demo"));
        assertThat(created.getResponse().getStatus()).isEqualTo(201);
        String token = tokenOf(created, "$.activationLink");
        assertThat(JsonPath.<String>read(created.getResponse().getContentAsString(), "$.employee.status"))
                .isEqualTo("PENDING");
        assertThat(login("new.user", PASSWORD)).isEqualTo(401);

        mockMvc.perform(get("/api/v1/auth/activation/" + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("new.user"))
                .andExpect(jsonPath("$.role").value("SALES"));

        String weak = "{\"token\":\"" + token + "\",\"password\":\"abc\"}";
        mockMvc.perform(post("/api/v1/auth/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(weak))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.password").exists());

        String strong = "{\"token\":\"" + token + "\",\"password\":\"" + NEW_PASSWORD + "\"}";
        mockMvc.perform(post("/api/v1/auth/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(strong))
                .andExpect(status().isOk());
        assertThat(login("new.user", NEW_PASSWORD)).isEqualTo(200);

        mockMvc.perform(post("/api/v1/auth/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(strong))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
        mockMvc.perform(get("/api/v1/auth/activation/" + token)).andExpect(status().isConflict());
    }

    @Test
    void tokensAreStoredHashedAndExpiredTokensAreRejected() throws Exception {
        MvcResult created = createEmployee(createBody("late.user", "late.user@pharmacy.demo"));
        String token = tokenOf(created, "$.activationLink");
        VerificationToken stored = tokenRepository.findAll().get(0);
        assertThat(stored.getTokenHash()).isNotEqualTo(token).hasSize(64);

        stored.setExpiresAt(Instant.now().minusSeconds(5));
        tokenRepository.save(stored);

        String body = "{\"token\":\"" + token + "\",\"password\":\"" + NEW_PASSWORD + "\"}";
        mockMvc.perform(post("/api/v1/auth/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
        mockMvc.perform(get("/api/v1/auth/activation/" + token)).andExpect(status().isConflict());
    }

    @Test
    void createRejectsDuplicateUsernameAndEmailAndBadInput() throws Exception {
        MvcResult duplicate = createEmployee(createBody("SALES", "SALES@pharmacy.demo"));
        assertThat(duplicate.getResponse().getStatus()).isEqualTo(409);
        String response = duplicate.getResponse().getContentAsString();
        assertThat(JsonPath.<String>read(response, "$.fieldErrors.username")).isNotBlank();
        assertThat(JsonPath.<String>read(response, "$.fieldErrors.email")).isNotBlank();

        MvcResult invalid = createEmployee("{\"name\":\"\",\"email\":\"x\",\"username\":\"a\",\"role\":\"SALES\"}");
        assertThat(invalid.getResponse().getStatus()).isEqualTo(400);
        assertThat(JsonPath.<String>read(invalid.getResponse().getContentAsString(), "$.fieldErrors.email"))
                .isNotBlank();
    }

    @Test
    void changingRoleRevokesTheExistingSession() throws Exception {
        Cookie salesSession = cookieOf("emp-sales");
        mockMvc.perform(get("/api/v1/auth/me").cookie(salesSession)).andExpect(status().isOk());

        patchJson("/api/v1/employees/emp-sales", "{\"role\":\"WAREHOUSE\"}", "emp-admin", 200);

        mockMvc.perform(get("/api/v1/auth/me").cookie(salesSession)).andExpect(status().isUnauthorized());
        assertThat(employeeRepository.findById("emp-sales").orElseThrow().getRole())
                .isEqualTo(Role.WAREHOUSE);
        patchJson("/api/v1/employees/emp-sales", "{\"role\":\"BOSS\"}", "emp-admin", 400);
    }

    @Test
    void lastActiveAdminCannotBeDemotedOrDisabledAndNobodyDemotesThemselves() throws Exception {
        patchJson("/api/v1/employees/emp-admin", "{\"role\":\"SALES\"}", "emp-admin", 409);
        patchJson("/api/v1/employees/emp-admin/status", "{\"status\":\"INACTIVE\"}", "emp-admin", 409);

        employeeRepository.save(employee("emp-admin2", "admin2", Role.ADMIN, EmployeeStatus.ACTIVE));
        patchJson("/api/v1/employees/emp-admin", "{\"role\":\"SALES\"}", "emp-admin", 409);
        patchJson("/api/v1/employees/emp-admin/status", "{\"status\":\"INACTIVE\"}", "emp-admin", 409);
        patchJson("/api/v1/employees/emp-admin2", "{\"role\":\"SALES\"}", "emp-admin", 200);
        patchJson("/api/v1/employees/emp-admin", "{\"role\":\"SALES\"}", "emp-admin", 409);
        assertThat(employeeRepository.findById("emp-admin").orElseThrow().getRole())
                .isEqualTo(Role.ADMIN);
    }

    @Test
    void disablingAnEmployeeBlocksLoginAndReactivationNeedsAPassword() throws Exception {
        patchJson("/api/v1/employees/emp-sales/status", "{\"status\":\"INACTIVE\"}", "emp-admin", 200);
        assertThat(login("sales", PASSWORD)).isEqualTo(403);
        patchJson("/api/v1/employees/emp-sales/status", "{\"status\":\"ACTIVE\"}", "emp-admin", 200);
        assertThat(login("sales", PASSWORD)).isEqualTo(200);
        patchJson("/api/v1/employees/emp-sales/status", "{\"status\":\"PENDING\"}", "emp-admin", 400);

        MvcResult created = createEmployee(createBody("never.set", "never.set@pharmacy.demo"));
        String id = JsonPath.read(created.getResponse().getContentAsString(), "$.employee.id");
        patchJson("/api/v1/employees/" + id + "/status", "{\"status\":\"ACTIVE\"}", "emp-admin", 409);
    }

    @Test
    void resetLinkChangesPasswordRevokesSessionsAndWorksOnce() throws Exception {
        Cookie salesSession = cookieOf("emp-sales");
        MvcResult issued = mockMvc.perform(
                        post("/api/v1/employees/emp-sales/reset-link").cookie(cookieOf("emp-admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resetLink").value(org.hamcrest.Matchers.containsString("reset-password.html")))
                .andReturn();
        String token = tokenOf(issued, "$.resetLink");

        mockMvc.perform(get("/api/v1/auth/reset-tokens/" + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));

        String body = "{\"token\":\"" + token + "\",\"password\":\"" + NEW_PASSWORD + "\"}";
        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        assertThat(login("sales", PASSWORD)).isEqualTo(401);
        assertThat(login("sales", NEW_PASSWORD)).isEqualTo(200);
        mockMvc.perform(get("/api/v1/auth/me").cookie(salesSession)).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
        mockMvc.perform(get("/api/v1/auth/reset-tokens/" + token)).andExpect(status().isConflict());
        mockMvc.perform(get("/api/v1/auth/reset-tokens/unknown")).andExpect(status().isConflict());
    }

    @Test
    void newResetLinkInvalidatesThePreviousOne() throws Exception {
        String first = tokenOf(
                mockMvc.perform(post("/api/v1/employees/emp-kho/reset-link").cookie(cookieOf("emp-admin")))
                        .andReturn(),
                "$.resetLink");
        String second = tokenOf(
                mockMvc.perform(post("/api/v1/employees/emp-kho/reset-link").cookie(cookieOf("emp-admin")))
                        .andReturn(),
                "$.resetLink");

        mockMvc.perform(get("/api/v1/auth/reset-tokens/" + first)).andExpect(status().isConflict());
        mockMvc.perform(get("/api/v1/auth/reset-tokens/" + second)).andExpect(status().isOk());
    }

    @Test
    void lockedAccountIsFlaggedAndCanBeUnlocked() throws Exception {
        Employee sales = employeeRepository.findById("emp-sales").orElseThrow();
        sales.setLockedUntil(Instant.now().plus(Duration.ofMinutes(10)));
        employeeRepository.save(sales);
        assertThat(login("sales", PASSWORD)).isEqualTo(403);

        mockMvc.perform(get("/api/v1/employees/emp-sales").cookie(cookieOf("emp-admin")))
                .andExpect(jsonPath("$.locked").value(true));
        mockMvc.perform(post("/api/v1/employees/emp-sales/unlock").cookie(cookieOf("emp-admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locked").value(false));
        assertThat(login("sales", PASSWORD)).isEqualTo(200);
    }
}
