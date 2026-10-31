package com.pharmacy.demo.bootstrap;

import com.pharmacy.demo.config.AppProperties;
import com.pharmacy.demo.entity.ActivityLog;
import com.pharmacy.demo.entity.Category;
import com.pharmacy.demo.entity.CodeCounter;
import com.pharmacy.demo.entity.Customer;
import com.pharmacy.demo.entity.Disposal;
import com.pharmacy.demo.entity.Employee;
import com.pharmacy.demo.entity.GoodsReceipt;
import com.pharmacy.demo.entity.Invoice;
import com.pharmacy.demo.entity.InvoiceDraft;
import com.pharmacy.demo.entity.LoyaltyTransaction;
import com.pharmacy.demo.entity.Medicine;
import com.pharmacy.demo.entity.StockBatch;
import com.pharmacy.demo.entity.StockMovement;
import com.pharmacy.demo.entity.Stocktake;
import com.pharmacy.demo.entity.Supplier;
import com.pharmacy.demo.entity.SystemSetting;
import com.pharmacy.demo.entity.VerificationToken;
import com.pharmacy.demo.repository.CategoryRepository;
import com.pharmacy.demo.repository.CustomerRepository;
import com.pharmacy.demo.repository.EmployeeRepository;
import com.pharmacy.demo.repository.MedicineRepository;
import com.pharmacy.demo.repository.SupplierRepository;
import com.pharmacy.demo.service.CodeGenerator;
import com.pharmacy.demo.service.CodeKind;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private static final long SEED = 20260427L;
    private static final Pattern TRAILING_NUMBER = Pattern.compile("(\\d+)$");

    private static final List<Class<?>> ALL_COLLECTIONS = List.of(
            ActivityLog.class,
            LoyaltyTransaction.class,
            InvoiceDraft.class,
            Invoice.class,
            Disposal.class,
            Stocktake.class,
            StockMovement.class,
            StockBatch.class,
            GoodsReceipt.class,
            Customer.class,
            Supplier.class,
            Medicine.class,
            Category.class,
            VerificationToken.class,
            CodeCounter.class,
            SystemSetting.class,
            Employee.class);

    private final AppProperties properties;
    private final MongoTemplate mongoTemplate;
    private final CatalogSeeder catalogSeeder;
    private final ReceiptSeeder receiptSeeder;
    private final CodeGenerator codeGenerator;
    private final EmployeeRepository employeeRepository;
    private final CategoryRepository categoryRepository;
    private final MedicineRepository medicineRepository;
    private final SupplierRepository supplierRepository;
    private final CustomerRepository customerRepository;
    private final Clock clock;

    @Override
    public void run(ApplicationArguments arguments) {
        if (!properties.seed().enabled()) {
            return;
        }
        if (employeeRepository.count() > 0) {
            log.info("Seed skipped: the system already has accounts");
            return;
        }
        clearAll();
        seed();
    }

    public void reset() {
        clearAll();
        seed();
    }

    private void clearAll() {
        ALL_COLLECTIONS.forEach(type -> mongoTemplate.remove(new Query(), type));
    }

    private void seed() {
        CatalogData catalog = catalogSeeder.build(new SeedRandom(SEED));
        categoryRepository.insert(catalog.categories());
        medicineRepository.insert(catalog.medicines());
        supplierRepository.insert(catalog.suppliers());
        customerRepository.insert(catalog.customers());
        raiseCounter(CodeKind.MEDICINE, catalog.medicines(), Medicine::getCode);
        raiseCounter(CodeKind.SUPPLIER, catalog.suppliers(), Supplier::getCode);
        raiseCounter(CodeKind.CUSTOMER, catalog.customers(), Customer::getCode);
        raiseCounter(CodeKind.EMPLOYEE, catalog.employees(), Employee::getCode);
        employeeRepository.insert(catalog.employees());
        int receipts = receiptSeeder.seed(catalog.medicines(), catalog.suppliers(), catalog.employees());
        log.info(
                "Seeded {} medicines, {} categories, {} suppliers, {} customers, {} employees, {} receipts",
                catalog.medicines().size(),
                catalog.categories().size(),
                catalog.suppliers().size(),
                catalog.customers().size(),
                catalog.employees().size(),
                receipts);
    }

    private <T> void raiseCounter(CodeKind kind, Collection<T> items, Function<T, String> codeOf) {
        long highest = items.stream()
                .map(codeOf)
                .map(TRAILING_NUMBER::matcher)
                .filter(Matcher::find)
                .mapToLong(matcher -> Long.parseLong(matcher.group(1)))
                .max()
                .orElse(0);
        if (highest > 0) {
            codeGenerator.raiseTo(kind, LocalDate.now(clock), highest);
        }
    }
}
