package com.pharmacy.demo.entity;

import com.pharmacy.demo.entity.embedded.DraftLine;
import com.pharmacy.demo.enums.DiscountType;
import com.pharmacy.demo.enums.PaymentMethod;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "gio_ban_nhap")
public class InvoiceDraft {

    @Id
    private String id;

    @Indexed()
    @Field("id_nhan_vien")
    private String employeeId;

    @Field("id_khach_hang")
    private String customerId;

    @Field("ten_khach_hang")
    private String customerName;

    @Field("danh_sach_thuoc")
    private List<DraftLine> items;

    @Field("hinh_thuc_giam_gia")
    private DiscountType discountType;

    @Field("gia_tri_giam_gia")
    private Long discountValue;

    @Field("phuong_thuc_thanh_toan")
    private PaymentMethod paymentMethod;

    @Field("ghi_chu")
    private String note;

    @Field("ngay_tao")
    private Instant createdAt;

    @Field("ngay_cap_nhat")
    private Instant savedAt;
}
