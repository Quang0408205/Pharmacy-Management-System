package com.pharmacy.demo.entity;

import com.pharmacy.demo.enums.BatchHandlingStatus;
import java.time.Instant;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndexes({
    @CompoundIndex(name = "dong_nhap_duy_nhat", def = "{'id_phieu_nhap': 1, 'so_dong_nhap': 1}", unique = true),
    @CompoundIndex(name = "so_lo_theo_thuoc", def = "{'id_thuoc': 1, 'so_lo': 1}", unique = true),
    @CompoundIndex(name = "thuoc_han_su_dung", def = "{'id_thuoc': 1, 'han_su_dung': 1}")
})
@Document(collection = "lo_thuoc")
public class StockBatch {

    @Id
    private String id;

    @Field("so_lo")
    private String code;

    @Field("id_thuoc")
    private String medicineId;

    @Field("id_phieu_nhap")
    private String receiptId;

    @Field("so_dong_nhap")
    private Integer receiptLineNo;

    @Field("id_nha_cung_cap")
    private String supplierId;

    @Field("ngay_san_xuat")
    private LocalDate manufactureDate;

    @Indexed()
    @Field("han_su_dung")
    private LocalDate expiryDate;

    @Field("ngay_nhap")
    private LocalDate receivedDate;

    @Field("so_luong_nhap")
    private int initialQty;

    @Field("so_luong_con")
    private int remainingQty;

    @Field("gia_nhap")
    private Long purchasePrice;

    @Field("trang_thai_xu_ly")
    private BatchHandlingStatus handlingStatus;

    @Field("phien_ban")
    private long ledgerVersion;

    @Field("ngay_tao")
    private Instant createdAt;

    @Field("ngay_cap_nhat")
    private Instant updatedAt;
}
