package com.pharmacy.demo.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pharmacy.demo.entity.Employee;
import com.pharmacy.demo.enums.EmployeeStatus;
import com.pharmacy.demo.enums.Role;
import com.pharmacy.demo.repository.EmployeeRepository;
import com.pharmacy.demo.support.MongoIntegrationTest;
import jakarta.servlet.http.Cookie;
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
class AuthControllerIT {

    private static final String PASSWORD = "Admin@12345";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void seedEmployees() {
        employeeRepository.deleteAll();
        employeeRepository.save(employee("emp-admin", "admin", Role.ADMIN, EmployeeStatus.ACTIVE));
        employeeRepository.save(employee("emp-off", "off", Role.SALES, EmployeeStatus.INACTIVE));
        employeeRepository.save(employee("emp-new", "newbie", Role.WAREHOUSE, EmployeeStatus.PENDING));
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

    private MvcResult login(String identifier, String password) throws Exception {
        String body = "{\"identifier\":\"" + identifier + "\",\"password\":\"" + password + "\",\"remember\":false}";
        return mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn();
    }

    private Cookie sessionCookie(MvcResult result) {
        String header = result.getResponse().getHeader("Set-Cookie");
        assertThat(header).isNotNull();
        String pair = header.substring(0, header.indexOf(';'));
        int separator = pair.indexOf('=');
        return new Cookie(pair.substring(0, separator), pair.substring(separator + 1));
    }

    @Test
    void loginIssuesHttpOnlyStrictCookieAndMeReturnsTheUser() throws Exception {
        MvcResult result = login("admin", PASSWORD);

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(result.getResponse().getHeader("Set-Cookie"))
                .contains("PMS_TOKEN=", "HttpOnly", "SameSite=Strict", "Path=/api");

        mockMvc.perform(get("/api/v1/auth/me").cookie(sessionCookie(result)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.username").value("admin"))
                .andExpect(jsonPath("$.user.role").value("ADMIN"))
                .andExpect(jsonPath("$.user.name").value("Người dùng admin"))
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist());
    }

    @Test
    void loginAcceptsEmailInAnyCase() throws Exception {
        assertThat(login("ADMIN@Pharmacy.demo", PASSWORD).getResponse().getStatus())
                .isEqualTo(200);
    }

    @Test
    void wrongPasswordIsRejectedWithoutRevealingWhichPartWasWrong() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"admin\",\"password\":\"sai\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"khong-ton-tai\",\"password\":\"sai\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void blankFieldsReturnFieldErrors() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.identifier").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    void accountIsLockedAfterTooManyFailuresEvenForTheCorrectPassword() throws Exception {
        for (int attempt = 0; attempt < 3; attempt++) {
            assertThat(login("admin", "sai").getResponse().getStatus()).isEqualTo(401);
        }

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"admin\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_LOCKED"));
    }

    @Test
    void inactiveAndPendingAccountsCannotLogIn() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"off\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_DISABLED"));
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"newbie\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_PENDING"));
    }

    @Test
    void sessionIsRevokedWhenTheSecurityVersionChanges() throws Exception {
        Cookie cookie = sessionCookie(login("admin", PASSWORD));
        mockMvc.perform(get("/api/v1/auth/me").cookie(cookie)).andExpect(status().isOk());

        Employee admin = employeeRepository.findById("emp-admin").orElseThrow();
        admin.setSecurityVersion(admin.getSecurityVersion() + 1);
        employeeRepository.save(admin);

        mockMvc.perform(get("/api/v1/auth/me").cookie(cookie)).andExpect(status().isUnauthorized());
    }

    @Test
    void sessionIsRevokedWhenTheAccountIsDeactivated() throws Exception {
        Cookie cookie = sessionCookie(login("admin", PASSWORD));

        Employee admin = employeeRepository.findById("emp-admin").orElseThrow();
        admin.setStatus(EmployeeStatus.INACTIVE);
        employeeRepository.save(admin);

        mockMvc.perform(get("/api/v1/auth/me").cookie(cookie)).andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointsRequireAuthenticationAndReturnJsonErrors() throws Exception {
        mockMvc.perform(get("/api/v1/medicines"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        mockMvc.perform(get("/api/v1/auth/me").cookie(new Cookie("PMS_TOKEN", "not-a-jwt")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutClearsTheCookie() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")));
    }
}
