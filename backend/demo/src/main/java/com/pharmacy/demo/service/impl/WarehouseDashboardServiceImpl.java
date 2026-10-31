package com.pharmacy.demo.service.impl;

import com.pharmacy.demo.dto.response.InventorySummaryResponse;
import com.pharmacy.demo.dto.response.WarehouseDashboardResponse;
import com.pharmacy.demo.entity.StockMovement;
import com.pharmacy.demo.enums.DisposalStatus;
import com.pharmacy.demo.enums.MovementType;
import com.pharmacy.demo.enums.ReceiptStatus;
import com.pharmacy.demo.enums.StocktakeStatus;
import com.pharmacy.demo.mapper.MovementMapper;
import com.pharmacy.demo.mapper.ReceiptMapper;
import com.pharmacy.demo.repository.DisposalRepository;
import com.pharmacy.demo.repository.GoodsReceiptRepository;
import com.pharmacy.demo.repository.StockMovementRepository;
import com.pharmacy.demo.repository.StocktakeRepository;
import com.pharmacy.demo.service.BatchQueryService;
import com.pharmacy.demo.service.InventoryService;
import com.pharmacy.demo.service.MedicineService;
import com.pharmacy.demo.service.WarehouseDashboardService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WarehouseDashboardServiceImpl implements WarehouseDashboardService {

    private static final int SERIES_DAYS = 14;
    private static final int LIST_SIZE = 6;
    private static final int RECEIPT_LIST_SIZE = 5;

    private final InventoryService inventoryService;
    private final MedicineService medicineService;
    private final BatchQueryService batchQueryService;
    private final GoodsReceiptRepository receiptRepository;
    private final StockMovementRepository movementRepository;
    private final StocktakeRepository stocktakeRepository;
    private final DisposalRepository disposalRepository;
    private final ReceiptMapper receiptMapper;
    private final MovementMapper movementMapper;
    private final MongoTemplate mongoTemplate;
    private final Clock clock;

    @Override
    public WarehouseDashboardResponse warehouse() {
        LocalDate today = LocalDate.now(clock);
        InventorySummaryResponse inventory = inventoryService.summary();
        long lowStock = inventory.lowStock() + inventory.outOfStock();
        long pendingReceipts = receiptRepository.countByStatus(ReceiptStatus.DRAFT);
        long openStocktakes = stocktakeRepository.countByStatus(StocktakeStatus.DRAFT)
                + stocktakeRepository.countByStatus(StocktakeStatus.PENDING_APPROVAL);
        long approvedDisposals = disposalRepository.countByStatus(DisposalStatus.APPROVED);
        Instant seriesStart =
                today.minusDays(SERIES_DAYS - 1L).atStartOfDay(clock.getZone()).toInstant();
        List<StockMovement> recent =
                mongoTemplate.find(Query.query(Criteria.where("createdAt").gte(seriesStart)), StockMovement.class);
        Instant startOfToday = today.atStartOfDay(clock.getZone()).toInstant();
        long movementsToday = recent.stream()
                .filter(movement -> !movement.getCreatedAt().isBefore(startOfToday))
                .count();
        PageRequest newestMovements = PageRequest.of(0, LIST_SIZE, Sort.by(Sort.Direction.DESC, "createdAt"));
        PageRequest newestReceipts = PageRequest.of(
                0,
                RECEIPT_LIST_SIZE,
                Sort.by(Sort.Direction.DESC, "receivingDate").and(Sort.by(Sort.Direction.DESC, "createdAt")));
        return new WarehouseDashboardResponse(
                new WarehouseDashboardResponse.Kpis(
                        inventory.activeMedicines(),
                        inventory.totalBatches(),
                        lowStock,
                        inventory.expiringBatches(),
                        pendingReceipts,
                        movementsToday),
                tasks(lowStock, inventory, pendingReceipts, openStocktakes, approvedDisposals),
                series(recent, today),
                medicineService.lowStock(LIST_SIZE),
                batchQueryService.expiringSoon(LIST_SIZE),
                receiptRepository.findAll(newestReceipts).stream()
                        .map(receiptMapper::toSummary)
                        .toList(),
                movementRepository.findAll(newestMovements).stream()
                        .map(movementMapper::toResponse)
                        .toList());
    }

    private List<WarehouseDashboardResponse.Task> tasks(
            long lowStock,
            InventorySummaryResponse inventory,
            long pendingReceipts,
            long openStocktakes,
            long approvedDisposals) {
        return List.of(
                new WarehouseDashboardResponse.Task(
                        "low",
                        "Thuốc sắp hết cần lên kế hoạch nhập",
                        lowStock,
                        "warning",
                        "inventory.html?stockStatus=LOW_STOCK"),
                new WarehouseDashboardResponse.Task(
                        "expiring",
                        "Lô sắp hết hạn cần ưu tiên xuất bán",
                        inventory.expiringBatches(),
                        "warning",
                        "expiry.html"),
                new WarehouseDashboardResponse.Task(
                        "expired",
                        "Lô đã hết hạn cần xử lý hủy",
                        inventory.expiredBatches(),
                        "danger",
                        "expiry.html?tier=expired"),
                new WarehouseDashboardResponse.Task(
                        "receipts",
                        "Phiếu nhập nháp chờ xác nhận",
                        pendingReceipts,
                        "info",
                        "receipts.html?status=DRAFT"),
                new WarehouseDashboardResponse.Task(
                        "stocktakes", "Phiếu kiểm kê đang xử lý", openStocktakes, "info", "stocktake.html"),
                new WarehouseDashboardResponse.Task(
                        "disposals",
                        "Phiếu xuất hủy đã duyệt chờ thực hiện",
                        approvedDisposals,
                        "danger",
                        "disposals.html"));
    }

    private List<WarehouseDashboardResponse.MovementPoint> series(List<StockMovement> movements, LocalDate today) {
        Map<LocalDate, long[]> buckets = new LinkedHashMap<>();
        for (int offset = SERIES_DAYS - 1; offset >= 0; offset--) {
            buckets.put(today.minusDays(offset), new long[2]);
        }
        for (StockMovement movement : movements) {
            long[] bucket =
                    buckets.get(movement.getCreatedAt().atZone(clock.getZone()).toLocalDate());
            if (bucket == null) {
                continue;
            }
            if (movement.getType() == MovementType.IN) {
                bucket[0] += movement.getQuantity();
            } else if (movement.getType() == MovementType.OUT) {
                bucket[1] += -movement.getQuantity();
            }
        }
        List<WarehouseDashboardResponse.MovementPoint> points = new ArrayList<>();
        buckets.forEach(
                (date, bucket) -> points.add(new WarehouseDashboardResponse.MovementPoint(date, bucket[0], bucket[1])));
        return points;
    }
}
