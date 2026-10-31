package com.pharmacy.demo.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pharmacy.demo.entity.Employee;
import com.pharmacy.demo.entity.Invoice;
import com.pharmacy.demo.entity.StockBatch;
import com.pharmacy.demo.entity.VerificationToken;
import com.pharmacy.demo.support.MongoIntegrationTest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.data.mongodb.core.query.Query;

@MongoIntegrationTest
class IndexCreationIT {

    @Autowired
    private MongoTemplate mongoTemplate;

    private List<IndexInfo> indexes(Class<?> type) {
        return mongoTemplate.indexOps(type).getIndexInfo();
    }

    @Test
    void uniqueIndexesAreCreatedFromEntityAnnotations() {
        assertThat(indexes(Employee.class))
                .anySatisfy(index -> {
                    assertThat(index.getName()).isEqualTo("ten_dang_nhap_chuan_hoa");
                    assertThat(index.isUnique()).isTrue();
                })
                .anySatisfy(index -> assertThat(index.getName()).isEqualTo("thu_dien_tu_chuan_hoa"));
        assertThat(indexes(StockBatch.class))
                .extracting(IndexInfo::getName)
                .contains("dong_nhap_duy_nhat", "so_lo_theo_thuoc", "thuoc_han_su_dung");
    }

    @Test
    void invoiceRequestIdIsUniqueOnlyWhenPresent() {
        IndexInfo requestIndex = indexes(Invoice.class).stream()
                .filter(index -> index.getName().equals("ma_yeu_cau"))
                .findFirst()
                .orElseThrow();

        assertThat(requestIndex.isUnique()).isTrue();
        assertThat(requestIndex.getPartialFilterExpression()).contains("ma_yeu_cau");
    }

    @Test
    void verificationTokensExpireThroughATtlIndex() {
        assertThat(indexes(VerificationToken.class))
                .anySatisfy(index -> assertThat(index.getExpireAfter()).isPresent());
    }

    @Test
    void duplicateUsernamesAreRejectedByTheDatabase() {
        mongoTemplate.remove(new Query(), Employee.class);
        mongoTemplate.insert(Employee.builder()
                .id("dup-a")
                .code("NV-A")
                .usernameNormalized("same")
                .emailNormalized("a@x.vn")
                .build());

        assertThatThrownBy(() -> mongoTemplate.insert(Employee.builder()
                        .id("dup-b")
                        .code("NV-B")
                        .usernameNormalized("same")
                        .emailNormalized("b@x.vn")
                        .build()))
                .isInstanceOf(DuplicateKeyException.class);

        mongoTemplate.remove(new Query(), Employee.class);
    }
}
