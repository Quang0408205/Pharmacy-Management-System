package com.pharmacy.demo.service.impl;

import com.pharmacy.demo.dto.response.NotificationResponse;
import com.pharmacy.demo.entity.StockBatch;
import com.pharmacy.demo.entity.SystemSetting;
import com.pharmacy.demo.enums.MedicineStatus;
import com.pharmacy.demo.enums.ReceiptStatus;
import com.pharmacy.demo.enums.Role;
import com.pharmacy.demo.enums.StockStatus;
import com.pharmacy.demo.enums.StocktakeStatus;
import com.pharmacy.demo.repository.GoodsReceiptRepository;
import com.pharmacy.demo.repository.MedicineRepository;
import com.pharmacy.demo.repository.StocktakeRepository;
import com.pharmacy.demo.security.Actor;
import com.pharmacy.demo.service.NotificationService;
import com.pharmacy.demo.service.StockQueryService;
import com.pharmacy.demo.service.StockSummary;
import com.pharmacy.demo.service.SystemSettingService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final MongoTemplate mongoTemplate;
    private final MedicineRepository medicineRepository;
    private final GoodsReceiptRepository receiptRepository;
    private final StocktakeRepository stocktakeRepository;
    private final StockQueryService stockQueryService;
    private final SystemSettingService settingService;
    private final Clock clock;

    @Override
    public List<NotificationResponse> list(Actor actor) {
        SystemSetting settings = settingService.current();
        LocalDate today = LocalDate.now(clock);
        LocalDate warningLimit = today.plusDays(settings.getExpiryWarningDays());
        String base = "pages/" + folderOf(actor.role()) + "/";
        List<NotificationResponse> items = new ArrayList<>();

        long lowStock = lowStockCount(settings);
        if (lowStock > 0) {
            items.add(new NotificationResponse(
                    "low-stock",
                    "warning",
                    lowStock + " thuốc sắp hết hoặc đã hết",
                    "Cần kiểm tra và lên kế hoạch nhập hàng.",
                    base + lowStockPage(actor.role())));
        }
        long expiring = countBatches(Criteria.where("expiryDate").gte(today).lte(warningLimit));
        if (expiring > 0) {
            items.add(new NotificationResponse(
                    "expiring",
                    "warning",
                    expiring + " lô sắp hết hạn",
                    "Trong vòng " + settings.getExpiryWarningDays() + " ngày tới.",
                    base + expiryPage(actor.role())));
        }
        long expired = countBatches(Criteria.where("expiryDate").lt(today));
        if (expired > 0) {
            items.add(new NotificationResponse(
                    "expired",
                    "danger",
                    expired + " lô đã hết hạn còn tồn",
                    "Không được bán, cần xử lý hủy theo quy trình.",
                    base + expiryPage(actor.role())));
        }
        if (actor.role() != Role.SALES) {
            long pendingReceipts = receiptRepository.countByStatus(ReceiptStatus.DRAFT);
            if (pendingReceipts > 0) {
                items.add(new NotificationResponse(
                        "receipts",
                        "info",
                        pendingReceipts + " phiếu nhập đang chờ",
                        "Phiếu nháp chưa được xác nhận nhập kho.",
                        base + "receipts.html"));
            }
        }
        if (actor.role() == Role.ADMIN) {
            long pendingStocktakes = stocktakeRepository.countByStatus(StocktakeStatus.PENDING_APPROVAL);
            if (pendingStocktakes > 0) {
                items.add(new NotificationResponse(
                        "stocktakes",
                        "info",
                        pendingStocktakes + " phiếu kiểm kê chờ duyệt",
                        "Cần quản trị viên duyệt điều chỉnh kho.",
                        "pages/admin/inventory.html?tab=stocktakes"));
            }
        }
        return items;
    }

    private long lowStockCount(SystemSetting settings) {
        Map<String, StockSummary> stock = stockQueryService.summarizeAll();
        return medicineRepository.findAll().stream()
                .filter(medicine -> medicine.getStatus() == MedicineStatus.ACTIVE)
                .filter(medicine -> stockQueryService.statusOf(
                                medicine, stock.getOrDefault(medicine.getId(), StockSummary.EMPTY), settings)
                        != StockStatus.IN_STOCK)
                .count();
    }

    private long countBatches(Criteria expiryCriteria) {
        Criteria criteria =
                new Criteria().andOperator(Criteria.where("remainingQty").gt(0), expiryCriteria);
        return mongoTemplate.count(Query.query(criteria), StockBatch.class);
    }

    private String folderOf(Role role) {
        return switch (role) {
            case ADMIN -> "admin";
            case SALES -> "sales";
            case WAREHOUSE -> "warehouse";
        };
    }

    private String lowStockPage(Role role) {
        return role == Role.SALES ? "lookup.html" : "inventory.html";
    }

    private String expiryPage(Role role) {
        return switch (role) {
            case ADMIN -> "inventory.html";
            case SALES -> "lookup.html";
            case WAREHOUSE -> "expiry.html";
        };
    }
}
