package com.pharmacy.demo.bootstrap;

import static com.pharmacy.demo.bootstrap.SeedFiles.integer;
import static com.pharmacy.demo.bootstrap.SeedFiles.number;
import static com.pharmacy.demo.bootstrap.SeedFiles.text;

import com.pharmacy.demo.dto.request.ReceiptItemRequest;
import com.pharmacy.demo.dto.request.ReceiptRequest;
import com.pharmacy.demo.entity.Employee;
import com.pharmacy.demo.entity.Medicine;
import com.pharmacy.demo.entity.Supplier;
import com.pharmacy.demo.enums.Role;
import com.pharmacy.demo.security.Actor;
import com.pharmacy.demo.service.ReceiptCommandService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

@Component
@RequiredArgsConstructor
public class ReceiptSeeder {

    public static final String FILE_NAME = "phieu_nhap.json";

    private final SeedFiles seedFiles;
    private final ReceiptCommandService receiptCommandService;
    private final Clock clock;

    public int seed(List<Medicine> medicines, List<Supplier> suppliers, List<Employee> employees) {
        Map<String, String> medicineIds =
                medicines.stream().collect(Collectors.toMap(Medicine::getCode, Medicine::getId));
        Map<String, String> supplierIds =
                suppliers.stream().collect(Collectors.toMap(Supplier::getCode, Supplier::getId));
        Actor actor = receiverOf(employees);
        LocalDate today = LocalDate.now(clock);
        List<JsonNode> receipts = new ArrayList<>(seedFiles.read(FILE_NAME));
        receipts.sort(Comparator.comparing((JsonNode node) -> integer(node, "ngay_nhap_truoc_so_ngay"))
                .reversed());
        for (JsonNode receipt : receipts) {
            LocalDate receivingDate = today.minusDays(integer(receipt, "ngay_nhap_truoc_so_ngay"));
            List<ReceiptItemRequest> items = new ArrayList<>();
            receipt.get("chi_tiet_phieu_nhap")
                    .forEach(line -> items.add(itemOf(line, receivingDate, today, medicineIds::get)));
            receiptCommandService.create(
                    new ReceiptRequest(
                            required(supplierIds.get(text(receipt, "ma_nha_cung_cap")), "supplier"),
                            receivingDate,
                            text(receipt, "ghi_chu"),
                            true,
                            items),
                    actor);
        }
        return receipts.size();
    }

    private static ReceiptItemRequest itemOf(
            JsonNode line, LocalDate receivingDate, LocalDate today, Function<String, String> medicineIdOf) {
        return new ReceiptItemRequest(
                required(medicineIdOf.apply(text(line, "ma_thuoc")), "medicine"),
                text(line, "so_lo"),
                receivingDate.minusDays(integer(line, "san_xuat_truoc_ngay_nhap")),
                today.plusDays(integer(line, "han_dung_sau_so_ngay")),
                integer(line, "so_luong"),
                number(line, "don_gia_nhap"));
    }

    private static Actor receiverOf(List<Employee> employees) {
        Employee receiver = employees.stream()
                .filter(employee -> employee.getRole() == Role.WAREHOUSE)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Seed data has no warehouse employee"));
        return new Actor(receiver.getId(), receiver.getFullName(), receiver.getRole());
    }

    private static String required(String value, String kind) {
        if (value == null) {
            throw new IllegalStateException("Seed receipt references an unknown " + kind);
        }
        return value;
    }
}
