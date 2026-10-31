package com.pharmacy.demo.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pharmacy.demo.entity.Employee;
import com.pharmacy.demo.entity.StockBatch;
import com.pharmacy.demo.enums.EmployeeStatus;
import com.pharmacy.demo.enums.ReceiptStatus;
import com.pharmacy.demo.enums.Role;
import com.pharmacy.demo.repository.CategoryRepository;
import com.pharmacy.demo.repository.CustomerRepository;
import com.pharmacy.demo.repository.DisposalRepository;
import com.pharmacy.demo.repository.EmployeeRepository;
import com.pharmacy.demo.repository.GoodsReceiptRepository;
import com.pharmacy.demo.repository.InvoiceRepository;
import com.pharmacy.demo.repository.MedicineRepository;
import com.pharmacy.demo.repository.StockBatchRepository;
import com.pharmacy.demo.repository.StockMovementRepository;
import com.pharmacy.demo.repository.StocktakeRepository;
import com.pharmacy.demo.repository.SupplierRepository;
import com.pharmacy.demo.service.BatchQueryService;
import com.pharmacy.demo.service.CodeGenerator;
import com.pharmacy.demo.service.CodeKind;
import com.pharmacy.demo.service.StockReconciliationService;
import com.pharmacy.demo.support.MongoIntegrationTest;
import jakarta.servlet.http.Cookie;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;

@MongoIntegrationTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.seed.enabled=true")
class DataSeederIT {

    private static final String PASSWORD = "Test@12345";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataSeeder dataSeeder;

    @Autowired
    private StockReconciliationService reconciliationService;

    @Autowired
    private CodeGenerator codeGenerator;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private MedicineRepository medicineRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private GoodsReceiptRepository receiptRepository;

    @Autowired
    private StockBatchRepository batchRepository;

    @Autowired
    private StockMovementRepository movementRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private StocktakeRepository stocktakeRepository;

    @Autowired
    private DisposalRepository disposalRepository;

    @Autowired
    private SeedFiles seedFiles;

    @Autowired
    private BatchQueryService batchQueryService;

    private Cookie login(String identifier) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"" + identifier + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String header = result.getResponse().getHeader("Set-Cookie");
        String pair = header.substring(0, header.indexOf(';'));
        int separator = pair.indexOf('=');
        return new Cookie(pair.substring(0, separator), pair.substring(separator + 1));
    }

    @Test
    void seedsReferenceDataAndOneAccountPerRole() {
        assertThat(employeeRepository.count()).isEqualTo(5);
        assertThat(employeeRepository
                        .findByUsernameNormalized("thulh")
                        .orElseThrow()
                        .getRole())
                .isEqualTo(Role.SALES);
        assertThat(employeeRepository
                        .findByUsernameNormalized("hungpv")
                        .orElseThrow()
                        .getRole())
                .isEqualTo(Role.WAREHOUSE);
        Employee admin = employeeRepository.findByUsernameNormalized("admin").orElseThrow();
        assertThat(admin.getFullName()).isEqualTo("Quang");
        assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
        assertThat(admin.getStatus()).isEqualTo(EmployeeStatus.ACTIVE);
        assertThat(categoryRepository.count()).isEqualTo(15);
        assertThat(supplierRepository.count()).isEqualTo(15);
        assertThat(customerRepository.count()).isEqualTo(100);
        assertThat(medicineRepository.count()).isEqualTo(2024);
    }

    @Test
    void seedsTheInitialStockFromTheReceiptFile() {
        List<JsonNode> receipts = seedFiles.read(ReceiptSeeder.FILE_NAME);
        List<JsonNode> lines = new ArrayList<>();
        receipts.forEach(receipt -> receipt.get("chi_tiet_phieu_nhap").forEach(lines::add));
        long units = lines.stream()
                .mapToLong(line -> line.get("so_luong").longValue())
                .sum();

        assertThat(receiptRepository.count()).isEqualTo(receipts.size());
        assertThat(receiptRepository.findAll()).allMatch(receipt -> receipt.getStatus() == ReceiptStatus.CONFIRMED);
        assertThat(batchRepository.count()).isEqualTo(lines.size());
        assertThat(batchRepository.findAll().stream()
                        .mapToLong(StockBatch::getRemainingQty)
                        .sum())
                .isEqualTo(units);
        assertThat(movementRepository.count()).isEqualTo(lines.size());
        assertThat(batchQueryService.expiringSoon(100)).isNotEmpty();
        assertThat(invoiceRepository.count()).isZero();
        assertThat(stocktakeRepository.count()).isZero();
        assertThat(disposalRepository.count()).isZero();
        assertThat(reconciliationService.findMismatches()).isEmpty();
    }

    @Test
    void generatedCodesNeverCollideWithSeededOnes() {
        assertThat(codeGenerator.next(CodeKind.MEDICINE)).isEqualTo("T2025");
        assertThat(codeGenerator.next(CodeKind.EMPLOYEE)).isEqualTo("NV006");
        assertThat(codeGenerator.next(CodeKind.SUPPLIER)).isEqualTo("NCC016");
        assertThat(codeGenerator.next(CodeKind.CUSTOMER)).isEqualTo("KH0101");
    }

    @Test
    void theAdministratorCanLogInAndUseTheSharedApis() throws Exception {
        Cookie cookie = login("admin");
        mockMvc.perform(get("/api/v1/medicines?size=5").cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2024));
        mockMvc.perform(get("/api/v1/categories").cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(15));
        mockMvc.perform(get("/api/v1/employees").cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(5));
        mockMvc.perform(get("/api/v1/inventory/summary").cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUnits").isNumber())
                .andExpect(jsonPath("$.activeMedicines").value(1927));
        mockMvc.perform(get("/api/v1/notifications").cookie(cookie)).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/settings").cookie(cookie)).andExpect(status().isOk());
    }

    @Test
    void runningTheSeederAgainDoesNotDuplicateOrDeleteData() throws Exception {
        long medicinesBefore = medicineRepository.count();
        long employeesBefore = employeeRepository.count();

        dataSeeder.run(null);

        assertThat(medicineRepository.count()).isEqualTo(medicinesBefore);
        assertThat(employeeRepository.count()).isEqualTo(employeesBefore);
    }

    @Test
    void adminCanResetAndGetTheSameSeedBack() throws Exception {
        Cookie admin = login("admin");

        mockMvc.perform(post("/api/v1/system/reset").cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertThat(medicineRepository.count()).isEqualTo(2024);
        assertThat(employeeRepository.count()).isEqualTo(5);
        assertThat(employeeRepository
                        .findByUsernameNormalized("thulh")
                        .orElseThrow()
                        .getRole())
                .isEqualTo(Role.SALES);
        assertThat(employeeRepository
                        .findByUsernameNormalized("hungpv")
                        .orElseThrow()
                        .getRole())
                .isEqualTo(Role.WAREHOUSE);
        assertThat(codeGenerator.next(CodeKind.MEDICINE)).isEqualTo("T2025");
    }
}
