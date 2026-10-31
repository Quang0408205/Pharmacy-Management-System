package com.pharmacy.demo.bootstrap;

import static com.pharmacy.demo.bootstrap.SeedFiles.integer;
import static com.pharmacy.demo.bootstrap.SeedFiles.number;
import static com.pharmacy.demo.bootstrap.SeedFiles.text;

import com.pharmacy.demo.config.AppProperties;
import com.pharmacy.demo.entity.Category;
import com.pharmacy.demo.entity.Customer;
import com.pharmacy.demo.entity.Employee;
import com.pharmacy.demo.entity.Medicine;
import com.pharmacy.demo.entity.Supplier;
import com.pharmacy.demo.entity.embedded.DataSource;
import com.pharmacy.demo.enums.CategoryStatus;
import com.pharmacy.demo.enums.CodedEnum;
import com.pharmacy.demo.enums.EmployeeStatus;
import com.pharmacy.demo.enums.MedicineStatus;
import com.pharmacy.demo.enums.PrescriptionType;
import com.pharmacy.demo.enums.Role;
import com.pharmacy.demo.enums.SupplierStatus;
import com.pharmacy.demo.service.SystemSettingService;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

@Component
@RequiredArgsConstructor
public class CatalogSeeder {

    private final SeedFiles files;
    private final SystemSettingService settingService;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties properties;
    private final Clock clock;

    public CatalogData build(SeedRandom random) {
        Instant now = clock.instant();
        String passwordHash = passwordEncoder.encode(requiredPassword());
        return new CatalogData(
                settingService.current(),
                categories(now),
                medicines(random, now),
                suppliers(random, now),
                customers(random, now),
                employees(random, now, passwordHash));
    }

    private String requiredPassword() {
        String password = properties.seed().defaultPassword();
        if (password == null || password.isBlank()) {
            throw new IllegalStateException("SEED_DEFAULT_PASSWORD must be set when seeding is enabled");
        }
        return password;
    }

    private List<Category> categories(Instant now) {
        return files.read("danh_muc.json").stream()
                .map(node -> Category.builder()
                        .id(text(node, "_id"))
                        .code(text(node, "ma_danh_muc"))
                        .name(text(node, "ten_danh_muc"))
                        .description(text(node, "mo_ta"))
                        .color(text(node, "mau_hien_thi"))
                        .status(CategoryStatus.ACTIVE)
                        .createdAt(now)
                        .updatedAt(now)
                        .build())
                .toList();
    }

    private List<Medicine> medicines(SeedRandom random, Instant now) {
        return files.read("thuoc.json").stream()
                .map(node -> Medicine.builder()
                        .id(text(node, "_id"))
                        .code(text(node, "ma_thuoc"))
                        .barcode(text(node, "ma_vach"))
                        .name(text(node, "ten_thuoc"))
                        .genericName(text(node, "ten_hoat_chat"))
                        .strength(text(node, "ham_luong"))
                        .dosageForm(text(node, "dang_bao_che"))
                        .categoryId(text(node, "id_danh_muc"))
                        .manufacturer(text(node, "nha_san_xuat"))
                        .country(text(node, "nuoc_san_xuat"))
                        .registrationNumber(text(node, "so_dang_ky"))
                        .unit(text(node, "don_vi_ban"))
                        .packaging(text(node, "quy_cach_dong_goi"))
                        .unitConversions(List.of())
                        .price(number(node, "gia_ban"))
                        .costPrice(number(node, "gia_nhap_tham_khao"))
                        .prescriptionType(code(PrescriptionType.class, text(node, "loai_ke_don")))
                        .minStock(integer(node, "muc_ton_toi_thieu"))
                        .imageUrl(text(node, "duong_dan_anh"))
                        .description(text(node, "mo_ta"))
                        .dataSource(dataSource(node.get("nguon_du_lieu")))
                        .status(code(MedicineStatus.class, text(node, "trang_thai")))
                        .createdAt(now.minus(random.between(200, 800), ChronoUnit.DAYS))
                        .updatedAt(now)
                        .build())
                .toList();
    }

    private DataSource dataSource(JsonNode node) {
        return DataSource.builder()
                .sourceType(text(node, "loai_nguon"))
                .sourceUrl(text(node, "duong_dan_nguon"))
                .collectedAt(Instant.parse(text(node, "thoi_diem_thu_thap")))
                .verified(node.get("da_xac_minh").booleanValue())
                .build();
    }

    private List<Supplier> suppliers(SeedRandom random, Instant now) {
        return files.read("nha_cung_cap.json").stream()
                .map(node -> Supplier.builder()
                        .id(text(node, "_id"))
                        .code(text(node, "ma_nha_cung_cap"))
                        .name(text(node, "ten_nha_cung_cap"))
                        .contactName(text(node, "nguoi_lien_he"))
                        .phone(text(node, "so_dien_thoai"))
                        .email(text(node, "thu_dien_tu"))
                        .address(text(node, "dia_chi"))
                        .taxCode(text(node, "ma_so_thue"))
                        .paymentTerms(text(node, "dieu_kien_thanh_toan"))
                        .note(text(node, "ghi_chu"))
                        .status(code(SupplierStatus.class, text(node, "trang_thai")))
                        .createdAt(now.minus(random.between(400, 900), ChronoUnit.DAYS))
                        .updatedAt(now)
                        .build())
                .toList();
    }

    private List<Customer> customers(SeedRandom random, Instant now) {
        return files.read("khach_hang.json").stream()
                .map(node -> Customer.builder()
                        .id(text(node, "_id"))
                        .code(text(node, "ma_khach_hang"))
                        .name(text(node, "ho_ten"))
                        .phone(text(node, "so_dien_thoai"))
                        .email(text(node, "thu_dien_tu"))
                        .address(text(node, "dia_chi"))
                        .gender(text(node, "gioi_tinh"))
                        .birthYear(integer(node, "nam_sinh"))
                        .note(text(node, "ghi_chu"))
                        .createdAt(now.minus(random.between(5, 400), ChronoUnit.DAYS))
                        .updatedAt(now)
                        .build())
                .toList();
    }

    private List<Employee> employees(SeedRandom random, Instant now, String passwordHash) {
        return files.read("nhan_vien.json").stream()
                .map(node -> {
                    String username = text(node, "ten_dang_nhap");
                    String email = text(node, "thu_dien_tu");
                    return Employee.builder()
                            .id(text(node, "_id"))
                            .code(text(node, "ma_nhan_vien"))
                            .username(username)
                            .usernameNormalized(username.toLowerCase(Locale.ROOT))
                            .fullName(text(node, "ho_ten"))
                            .email(email)
                            .emailNormalized(email.toLowerCase(Locale.ROOT))
                            .phone(text(node, "so_dien_thoai"))
                            .passwordHash(passwordHash)
                            .role(code(Role.class, text(node, "vai_tro")))
                            .status(code(EmployeeStatus.class, text(node, "trang_thai")))
                            .securityVersion(1L)
                            .createdAt(now.minus(random.between(100, 300), ChronoUnit.DAYS))
                            .updatedAt(now)
                            .build();
                })
                .toList();
    }

    private static <E extends Enum<E> & CodedEnum> E code(Class<E> type, String dbCode) {
        for (E constant : type.getEnumConstants()) {
            if (constant.getDbCode().equals(dbCode)) {
                return constant;
            }
        }
        throw new IllegalStateException("Unknown " + type.getSimpleName() + " code " + dbCode);
    }
}
