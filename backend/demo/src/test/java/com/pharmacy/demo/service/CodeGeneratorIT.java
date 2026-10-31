package com.pharmacy.demo.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.pharmacy.demo.support.MongoIntegrationTest;
import java.time.Clock;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;

@MongoIntegrationTest
class CodeGeneratorIT {

    @Autowired
    private CodeGenerator codeGenerator;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private Clock clock;

    @BeforeEach
    void clean() {
        mongoTemplate.dropCollection("bo_dem_ma");
    }

    @Test
    void generatesSequentialCodesWithConfiguredFormat() {
        LocalDate today = LocalDate.now(clock);

        assertThat(codeGenerator.next(CodeKind.MEDICINE)).isEqualTo("T0001");
        assertThat(codeGenerator.next(CodeKind.MEDICINE)).isEqualTo("T0002");
        assertThat(codeGenerator.next(CodeKind.RECEIPT)).isEqualTo("PN-" + today.getYear() + "-0001");
        assertThat(codeGenerator.next(CodeKind.INVOICE))
                .isEqualTo("HD-" + today.toString().replace("-", "") + "-0001");
    }

    @Test
    void previewDoesNotConsumeTheCounter() {
        assertThat(codeGenerator.preview(CodeKind.CUSTOMER)).isEqualTo("KH0001");
        assertThat(codeGenerator.preview(CodeKind.CUSTOMER)).isEqualTo("KH0001");
        assertThat(codeGenerator.next(CodeKind.CUSTOMER)).isEqualTo("KH0001");
        assertThat(codeGenerator.preview(CodeKind.CUSTOMER)).isEqualTo("KH0002");
    }

    @Test
    void raiseToNeverLowersAnExistingCounter() {
        LocalDate today = LocalDate.now(clock);
        codeGenerator.raiseTo(CodeKind.SUPPLIER, today, 15);
        codeGenerator.raiseTo(CodeKind.SUPPLIER, today, 10);

        assertThat(codeGenerator.next(CodeKind.SUPPLIER)).isEqualTo("NCC016");
    }

    @Test
    void concurrentCallersNeverReceiveTheSameCode() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(8);
        try {
            List<Callable<String>> tasks = IntStream.range(0, 40)
                    .<Callable<String>>mapToObj(index -> () -> codeGenerator.next(CodeKind.EMPLOYEE))
                    .toList();
            Set<String> codes = new HashSet<>();
            for (Future<String> future : executor.invokeAll(tasks)) {
                codes.add(future.get());
            }

            assertThat(codes).hasSize(40);
        } finally {
            executor.shutdownNow();
        }
    }
}
