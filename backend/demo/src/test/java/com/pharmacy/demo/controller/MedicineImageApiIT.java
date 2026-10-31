package com.pharmacy.demo.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pharmacy.demo.entity.Category;
import com.pharmacy.demo.entity.Employee;
import com.pharmacy.demo.entity.Medicine;
import com.pharmacy.demo.enums.CategoryStatus;
import com.pharmacy.demo.enums.EmployeeStatus;
import com.pharmacy.demo.enums.MedicineStatus;
import com.pharmacy.demo.enums.PrescriptionType;
import com.pharmacy.demo.enums.Role;
import com.pharmacy.demo.repository.CategoryRepository;
import com.pharmacy.demo.repository.EmployeeRepository;
import com.pharmacy.demo.repository.MedicineRepository;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@MongoIntegrationTest
@AutoConfigureMockMvc
class MedicineImageApiIT {

    private static final byte[] PNG = {
        (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D, 'I', 'H', 'D', 'R'
    };
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10, 'J', 'F', 'I', 'F'
    };

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private MedicineRepository medicineRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private JwtService jwtService;

    @BeforeEach
    void seed() {
        medicineRepository.deleteAll();
        categoryRepository.deleteAll();
        employeeRepository.deleteAll();
        employeeRepository.save(employee("emp-admin", "admin", Role.ADMIN));
        employeeRepository.save(employee("emp-sales", "sales", Role.SALES));
        categoryRepository.save(Category.builder()
                .id("cat-1")
                .code("KS")
                .name("Kháng sinh")
                .description("")
                .color("#0d9488")
                .status(CategoryStatus.ACTIVE)
                .createdAt(Instant.now())
                .build());
        medicineRepository.save(medicine("med-1", "T0001", "Công ty Dược Hậu Giang"));
        medicineRepository.save(medicine("med-2", "T0002", "Công ty Imexpharm"));
        medicineRepository.save(medicine("med-3", "T0003", "Công ty Dược Hậu Giang"));
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

    private Medicine medicine(String id, String code, String manufacturer) {
        return Medicine.builder()
                .id(id)
                .code(code)
                .name("Thuốc " + code)
                .unit("Hộp")
                .manufacturer(manufacturer)
                .categoryId("cat-1")
                .price(10000L)
                .costPrice(5000L)
                .minStock(5)
                .prescriptionType(PrescriptionType.OTC)
                .status(MedicineStatus.ACTIVE)
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

    private ResultActions upload(String actorId, String medicineId, String name, byte[] bytes) throws Exception {
        Employee employee = employeeRepository.findById(actorId).orElseThrow();
        return mockMvc.perform(multipart("/api/v1/medicines/" + medicineId + "/image")
                .file(new MockMultipartFile("image", name, "application/octet-stream", bytes))
                .cookie(new Cookie("PMS_TOKEN", jwtService.issue(employee, Duration.ofMinutes(10)))));
    }

    private static String medicineBody(String unit) {
        return "{\"code\":\"T0100\",\"name\":\"Paracetamol 500\",\"unit\":\"" + unit
                + "\",\"manufacturer\":\"Dược Hậu Giang\",\"categoryId\":\"cat-1\",\"price\":5000}";
    }

    @Test
    void adminUploadsReadsReplacesAndDeletesTheImage() throws Exception {
        send(get("/api/v1/medicines/med-1/image"), "emp-sales", null).andExpect(status().isNotFound());

        upload("emp-admin", "med-1", "a.png", PNG)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageUrl")
                        .value(org.hamcrest.Matchers.startsWith("/api/v1/medicines/med-1/image?v=")));
        send(get("/api/v1/medicines/med-1/image"), "emp-sales", null)
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(PNG));

        upload("emp-admin", "med-1", "b.jpg", JPEG).andExpect(status().isOk());
        send(get("/api/v1/medicines/med-1/image"), "emp-sales", null)
                .andExpect(content().contentType(MediaType.IMAGE_JPEG))
                .andExpect(content().bytes(JPEG));

        send(delete("/api/v1/medicines/med-1/image"), "emp-admin", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageUrl").doesNotExist());
        send(get("/api/v1/medicines/med-1/image"), "emp-sales", null).andExpect(status().isNotFound());
        assertThat(medicineRepository.findById("med-1").orElseThrow().getImageUrl())
                .isNull();
    }

    @Test
    void rejectsFilesThatAreNotRealImagesOrAreTooLarge() throws Exception {
        upload("emp-admin", "med-1", "x.png", "not an image at all".getBytes())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.image").exists());
        upload("emp-admin", "med-1", "x.png", new byte[0]).andExpect(status().isBadRequest());
        byte[] huge = new byte[2 * 1024 * 1024 + 10];
        System.arraycopy(JPEG, 0, huge, 0, JPEG.length);
        upload("emp-admin", "med-1", "huge.jpg", huge)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.image").exists());
        upload("emp-admin", "med-missing", "a.png", PNG).andExpect(status().isNotFound());
        send(get("/api/v1/medicines/med-1/image"), "emp-sales", null).andExpect(status().isNotFound());
    }

    @Test
    void onlyAdminsMayChangeImages() throws Exception {
        upload("emp-sales", "med-1", "a.png", PNG).andExpect(status().isForbidden());
        send(delete("/api/v1/medicines/med-1/image"), "emp-sales", null).andExpect(status().isForbidden());
    }

    @Test
    void suggestsDistinctManufacturersIgnoringAccents() throws Exception {
        send(get("/api/v1/medicines/manufacturers"), "emp-admin", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0]").value("Công ty Dược Hậu Giang"));
        send(get("/api/v1/medicines/manufacturers?q=imex"), "emp-admin", null)
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0]").value("Công ty Imexpharm"));
        send(get("/api/v1/medicines/manufacturers"), "emp-sales", null).andExpect(status().isForbidden());
    }

    @Test
    void onlyUnitsFromTheFixedListAreAccepted() throws Exception {
        send(post("/api/v1/medicines"), "emp-admin", medicineBody("Thùng"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.unit").exists());
        send(post("/api/v1/medicines"), "emp-admin", medicineBody("Tuýp")).andExpect(status().isCreated());
    }
}
