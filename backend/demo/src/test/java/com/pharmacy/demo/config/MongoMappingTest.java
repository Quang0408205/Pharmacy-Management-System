package com.pharmacy.demo.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.pharmacy.demo.entity.Employee;
import com.pharmacy.demo.entity.Invoice;
import com.pharmacy.demo.entity.StockBatch;
import com.pharmacy.demo.entity.embedded.BatchAllocation;
import com.pharmacy.demo.entity.embedded.InvoiceLine;
import com.pharmacy.demo.enums.BatchHandlingStatus;
import com.pharmacy.demo.enums.EmployeeStatus;
import com.pharmacy.demo.enums.InvoiceStatus;
import com.pharmacy.demo.enums.PrescriptionType;
import com.pharmacy.demo.enums.Role;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.NoOpDbRefResolver;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

class MongoMappingTest {

    private MappingMongoConverter converter;

    @BeforeEach
    void setUp() {
        MongoMappingContext context = new MongoMappingContext();
        context.setSimpleTypeHolder(new MongoConfig().mongoCustomConversions().getSimpleTypeHolder());
        context.afterPropertiesSet();
        converter = new MappingMongoConverter(NoOpDbRefResolver.INSTANCE, context);
        converter.setCustomConversions(new MongoConfig().mongoCustomConversions());
        converter.afterPropertiesSet();
    }

    @Test
    void writesEnumsWithVietnameseCodesAndReadsThemBack() {
        Employee employee = Employee.builder()
                .id("emp-01")
                .username("admin")
                .role(Role.ADMIN)
                .status(EmployeeStatus.PENDING)
                .securityVersion(3L)
                .createdAt(Instant.parse("2026-10-30T03:00:00Z"))
                .build();

        Document document = new Document();
        converter.write(employee, document);

        assertThat(document.get("_id")).isEqualTo("emp-01");
        assertThat(document.get("vai_tro")).isEqualTo("QUAN_TRI");
        assertThat(document.get("trang_thai")).isEqualTo("CHO_KICH_HOAT");
        assertThat(document.get("ten_dang_nhap")).isEqualTo("admin");
        assertThat(document.get("phien_ban_bao_mat")).isEqualTo(3L);

        Employee restored = converter.read(Employee.class, document);
        assertThat(restored.getRole()).isEqualTo(Role.ADMIN);
        assertThat(restored.getStatus()).isEqualTo(EmployeeStatus.PENDING);
        assertThat(restored.getCreatedAt()).isEqualTo(Instant.parse("2026-10-30T03:00:00Z"));
    }

    @Test
    void storesCalendarDatesAsUtcMidnightRegardlessOfSystemZone() {
        StockBatch batch = StockBatch.builder()
                .id("bat-1")
                .expiryDate(LocalDate.of(2028, 1, 5))
                .handlingStatus(BatchHandlingStatus.QUARANTINED)
                .remainingQty(150)
                .purchasePrice(12000L)
                .build();

        Document document = new Document();
        converter.write(batch, document);

        assertThat(document.get("han_su_dung")).isEqualTo(Date.from(Instant.parse("2028-01-05T00:00:00Z")));
        assertThat(document.get("trang_thai_xu_ly")).isEqualTo("CACH_LY");
        assertThat(document.get("so_luong_con")).isEqualTo(150);
        assertThat(document.get("gia_nhap")).isEqualTo(12000L);

        StockBatch restored = converter.read(StockBatch.class, document);
        assertThat(restored.getExpiryDate()).isEqualTo(LocalDate.of(2028, 1, 5));
        assertThat(restored.getHandlingStatus()).isEqualTo(BatchHandlingStatus.QUARANTINED);
    }

    @Test
    void mapsNestedInvoiceLinesAndAllocations() {
        Invoice invoice = Invoice.builder()
                .id("inv-1")
                .status(InvoiceStatus.CANCELLED)
                .total(36000L)
                .items(List.of(InvoiceLine.builder()
                        .medicineId("med-1")
                        .prescriptionType(PrescriptionType.CONTROLLED)
                        .quantity(2)
                        .allocations(List.of(BatchAllocation.builder()
                                .batchId("bat-1")
                                .expiryDate(LocalDate.of(2028, 1, 5))
                                .quantity(2)
                                .build()))
                        .build()))
                .build();

        Document document = new Document();
        converter.write(invoice, document);

        assertThat(document.get("trang_thai")).isEqualTo("DA_HUY");
        assertThat(document.get("tong_thanh_toan")).isEqualTo(36000L);
        Document line = ((List<Document>) document.get("chi_tiet_hoa_don")).get(0);
        assertThat(line.get("loai_ke_don")).isEqualTo("KIEM_SOAT_DAC_BIET");
        Document allocation = ((List<Document>) line.get("phan_bo_lo")).get(0);
        assertThat(allocation.get("id_lo_thuoc")).isEqualTo("bat-1");
        assertThat(allocation.get("so_luong_xuat")).isEqualTo(2);

        Invoice restored = converter.read(Invoice.class, document);
        assertThat(restored.getItems().get(0).getPrescriptionType()).isEqualTo(PrescriptionType.CONTROLLED);
        assertThat(restored.getItems().get(0).getAllocations().get(0).getExpiryDate())
                .isEqualTo(LocalDate.of(2028, 1, 5));
        assertThat(document.values()).noneMatch(Decimal128.class::isInstance);
    }
}
