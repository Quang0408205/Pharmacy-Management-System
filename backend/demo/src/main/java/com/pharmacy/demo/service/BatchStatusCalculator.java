package com.pharmacy.demo.service;

import com.pharmacy.demo.entity.StockBatch;
import com.pharmacy.demo.entity.SystemSetting;
import com.pharmacy.demo.enums.BatchHandlingStatus;
import com.pharmacy.demo.enums.BatchStatus;
import java.time.Clock;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BatchStatusCalculator {

    private static final double LOW_STOCK_RATIO = 0.2;

    private final Clock clock;

    public BatchStatus statusOf(StockBatch batch, SystemSetting settings) {
        LocalDate today = LocalDate.now(clock);
        if (batch.getHandlingStatus() == BatchHandlingStatus.QUARANTINED) {
            return BatchStatus.QUARANTINED;
        }
        if (batch.getExpiryDate() != null && batch.getExpiryDate().isBefore(today)) {
            return BatchStatus.EXPIRED;
        }
        if (batch.getRemainingQty() <= 0) {
            return BatchStatus.OUT_OF_STOCK;
        }
        if (batch.getExpiryDate() != null
                && !batch.getExpiryDate().isAfter(today.plusDays(settings.getExpiryWarningDays()))) {
            return BatchStatus.EXPIRING_SOON;
        }
        if (batch.getRemainingQty() <= batch.getInitialQty() * LOW_STOCK_RATIO) {
            return BatchStatus.LOW_STOCK;
        }
        return BatchStatus.AVAILABLE;
    }
}
