package com.pharmacy.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pharmacy.demo.entity.StockBatch;
import com.pharmacy.demo.enums.BatchHandlingStatus;
import com.pharmacy.demo.enums.MovementType;
import com.pharmacy.demo.enums.ReferenceType;
import com.pharmacy.demo.enums.Role;
import com.pharmacy.demo.exception.ConflictException;
import com.pharmacy.demo.repository.StockBatchRepository;
import com.pharmacy.demo.repository.StockMovementRepository;
import com.pharmacy.demo.security.Actor;
import com.pharmacy.demo.support.MongoIntegrationTest;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@MongoIntegrationTest
class StockLedgerServiceIT {

    private static final Actor ACTOR = new Actor("emp-1", "Nhân viên kho", Role.WAREHOUSE);

    @Autowired
    private StockLedgerService ledger;

    @Autowired
    private StockBatchRepository batchRepository;

    @Autowired
    private StockMovementRepository movementRepository;

    @BeforeEach
    void clean() {
        movementRepository.deleteAll();
        batchRepository.deleteAll();
    }

    private StockBatch receive(String id, int quantity) {
        return ledger.receiveBatch(
                StockBatch.builder()
                        .id(id)
                        .code("LOT-" + id)
                        .medicineId("med-1")
                        .receiptId("rec-1")
                        .receiptLineNo(1)
                        .supplierId("sup-1")
                        .expiryDate(LocalDate.now().plusYears(1))
                        .receivedDate(LocalDate.now())
                        .initialQty(quantity)
                        .purchasePrice(1000L)
                        .build(),
                ReferenceType.RECEIPT,
                "rec-1",
                "PN-TEST-0001",
                ACTOR,
                "Nhập thử");
    }

    private StockChange sale(String batchId, int quantity) {
        return StockChange.builder()
                .batchId(batchId)
                .delta(-quantity)
                .type(MovementType.OUT)
                .referenceType(ReferenceType.INVOICE)
                .referenceId("inv-1")
                .referenceCode("HD-TEST-0001")
                .actor(ACTOR)
                .allowedStatuses(Set.of(BatchHandlingStatus.AVAILABLE))
                .build();
    }

    @Test
    void receivingABatchWritesAnInMovementWithTheOpeningBalance() {
        StockBatch batch = receive("bat-1", 100);

        assertThat(batch.getRemainingQty()).isEqualTo(100);
        assertThat(movementRepository.findByBatchIdOrderByCreatedAtDesc("bat-1"))
                .singleElement()
                .satisfies(movement -> {
                    assertThat(movement.getType()).isEqualTo(MovementType.IN);
                    assertThat(movement.getQuantity()).isEqualTo(100);
                    assertThat(movement.getBalanceAfter()).isEqualTo(100);
                });
    }

    @Test
    void decreaseUpdatesQuantityVersionAndLedger() {
        receive("bat-1", 100);

        StockBatch updated = ledger.apply(sale("bat-1", 30));

        assertThat(updated.getRemainingQty()).isEqualTo(70);
        assertThat(updated.getLedgerVersion()).isEqualTo(1L);
        assertThat(movementRepository.findByBatchIdOrderByCreatedAtDesc("bat-1"))
                .hasSize(2);
    }

    @Test
    void decreaseBeyondRemainingQuantityIsRejectedAndWritesNothing() {
        receive("bat-1", 5);

        assertThatThrownBy(() -> ledger.apply(sale("bat-1", 6))).isInstanceOf(ConflictException.class);

        assertThat(batchRepository.findById("bat-1").orElseThrow().getRemainingQty())
                .isEqualTo(5);
        assertThat(movementRepository.findByBatchIdOrderByCreatedAtDesc("bat-1"))
                .hasSize(1);
    }

    @Test
    void quarantinedBatchCannotBeSold() {
        receive("bat-1", 10);
        ledger.changeHandlingStatus("bat-1", BatchHandlingStatus.QUARANTINED, Set.of(BatchHandlingStatus.AVAILABLE));

        assertThatThrownBy(() -> ledger.apply(sale("bat-1", 1))).isInstanceOf(ConflictException.class);
    }

    @Test
    void staleExpectedVersionIsRejected() {
        receive("bat-1", 10);
        ledger.apply(sale("bat-1", 1));

        StockChange stale = StockChange.builder()
                .batchId("bat-1")
                .delta(-1)
                .type(MovementType.ADJUSTMENT)
                .referenceType(ReferenceType.STOCKTAKE)
                .actor(ACTOR)
                .expectedVersion(0L)
                .build();

        assertThatThrownBy(() -> ledger.apply(stale)).isInstanceOf(ConflictException.class);
    }

    @Test
    void concurrentSalesNeverDriveTheBatchNegative() throws Exception {
        receive("bat-1", 10);
        AtomicInteger succeeded = new AtomicInteger();
        ExecutorService executor = Executors.newFixedThreadPool(8);
        try {
            List<Callable<Boolean>> tasks = IntStream.range(0, 25)
                    .<Callable<Boolean>>mapToObj(index -> () -> {
                        try {
                            ledger.apply(sale("bat-1", 1));
                            succeeded.incrementAndGet();
                            return true;
                        } catch (ConflictException exception) {
                            return false;
                        }
                    })
                    .toList();
            for (Future<Boolean> future : executor.invokeAll(tasks)) {
                future.get();
            }
        } finally {
            executor.shutdownNow();
        }

        assertThat(succeeded.get()).isEqualTo(10);
        assertThat(batchRepository.findById("bat-1").orElseThrow().getRemainingQty())
                .isZero();
        assertThat(movementRepository.findByBatchIdOrderByCreatedAtDesc("bat-1"))
                .hasSize(11);
    }
}
