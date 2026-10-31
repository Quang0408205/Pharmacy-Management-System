package com.pharmacy.demo.service.impl;

import com.pharmacy.demo.dto.response.ExpirySummaryResponse;
import com.pharmacy.demo.dto.response.InventorySummaryResponse;
import com.pharmacy.demo.entity.Medicine;
import com.pharmacy.demo.entity.StockBatch;
import com.pharmacy.demo.entity.SystemSetting;
import com.pharmacy.demo.enums.MedicineStatus;
import com.pharmacy.demo.enums.StockStatus;
import com.pharmacy.demo.repository.MedicineRepository;
import com.pharmacy.demo.repository.StockBatchRepository;
import com.pharmacy.demo.service.InventoryService;
import com.pharmacy.demo.service.StockQueryService;
import com.pharmacy.demo.service.StockSummary;
import com.pharmacy.demo.service.SystemSettingService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final MedicineRepository medicineRepository;
    private final StockBatchRepository batchRepository;
    private final StockQueryService stockQueryService;
    private final SystemSettingService settingService;
    private final Clock clock;

    @Override
    public InventorySummaryResponse summary() {
        SystemSetting settings = settingService.current();
        LocalDate today = LocalDate.now(clock);
        LocalDate warningLimit = today.plusDays(settings.getExpiryWarningDays());
        List<Medicine> medicines = medicineRepository.findAll();
        List<Medicine> active = medicines.stream()
                .filter(medicine -> medicine.getStatus() == MedicineStatus.ACTIVE)
                .toList();
        List<StockBatch> withStock = batchRepository.findAll().stream()
                .filter(batch -> batch.getRemainingQty() > 0)
                .toList();
        Map<String, StockSummary> stock = stockQueryService.summarizeAll();
        return new InventorySummaryResponse(
                medicines.size(),
                active.size(),
                withStock.size(),
                withStock.stream().mapToLong(StockBatch::getRemainingQty).sum(),
                withStock.stream()
                        .mapToLong(batch -> batch.getRemainingQty() * valueOf(batch.getPurchasePrice()))
                        .sum(),
                countStatus(active, stock, settings, StockStatus.LOW_STOCK),
                countStatus(active, stock, settings, StockStatus.OUT_OF_STOCK),
                withStock.stream()
                        .filter(batch -> !batch.getExpiryDate().isBefore(today)
                                && !batch.getExpiryDate().isAfter(warningLimit))
                        .count(),
                withStock.stream()
                        .filter(batch -> batch.getExpiryDate().isBefore(today))
                        .count());
    }

    @Override
    public ExpirySummaryResponse expirySummary() {
        SystemSetting settings = settingService.current();
        LocalDate today = LocalDate.now(clock);
        List<StockBatch> withStock = batchRepository.findAll().stream()
                .filter(batch -> batch.getRemainingQty() > 0)
                .toList();
        List<ExpirySummaryResponse.Tier> tiers = settings.getExpiryTiers().stream()
                .map(days -> new ExpirySummaryResponse.Tier(
                        days,
                        withStock.stream()
                                .filter(batch -> !batch.getExpiryDate().isBefore(today)
                                        && !batch.getExpiryDate().isAfter(today.plusDays(days)))
                                .count()))
                .toList();
        long expired = withStock.stream()
                .filter(batch -> batch.getExpiryDate().isBefore(today))
                .count();
        return new ExpirySummaryResponse(tiers, expired);
    }

    private long countStatus(
            List<Medicine> medicines, Map<String, StockSummary> stock, SystemSetting settings, StockStatus target) {
        return medicines.stream()
                .filter(medicine -> stockQueryService.statusOf(
                                medicine, stock.getOrDefault(medicine.getId(), StockSummary.EMPTY), settings)
                        == target)
                .count();
    }

    private static long valueOf(Long value) {
        return value == null ? 0 : value;
    }
}
