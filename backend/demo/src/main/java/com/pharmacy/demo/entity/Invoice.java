package com.pharmacy.demo.entity;

import com.pharmacy.demo.entity.embedded.InvoiceLine;
import com.pharmacy.demo.enums.DiscountType;
import com.pharmacy.demo.enums.InvoiceStatus;
import com.pharmacy.demo.enums.PaymentMethod;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndex(name = "nhan_vien_ngay_tao", def = "{'id_nhan_vien_ban': 1, 'ngay_tao': -1}")
@Document(collection = "hoa_don_ban_thuoc")
public class Invoice {

    @Id
    private String id;

    @Indexed(unique = true)
    @Field("ma_hoa_don")
    private String code;

    @Field("id_nhan_vien_ban")
    private String employeeId;

    @Field("ten_nhan_vien")
    private String employeeName;

    @Indexed()
    @Field("id_khach_hang")
    private String customerId;

    @Field("ten_khach_hang")
    private String customerName;

    @Field("chi_tiet_hoa_don")
    private List<InvoiceLine> items;

    @Field("tong_tien_hang")
    private Long subtotal;

    @Field("hinh_thuc_giam_gia")
    private DiscountType discountType;

    @Field("gia_tri_giam_gia")
    private Long discountValue;

    @Field("so_tien_giam")
    private Long discountAmount;

    @Field("diem_su_dung")
    private int pointsUsed;

    @Field("tien_giam_diem")
    private long pointsDiscount;

    @Field("diem_cong")
    private int pointsEarned;

    @Field("tong_thanh_toan")
    private Long total;

    @Field("phuong_thuc_thanh_toan")
    private PaymentMethod paymentMethod;

    @Field("so_tien_khach_tra")
    private Long receivedAmount;

    @Field("tien_thua")
    private Long changeAmount;

    @Field("da_xac_nhan_don_thuoc")
    private boolean prescriptionConfirmed;

    @Field("ma_don_thuoc")
    private String prescriptionCode;

    @Field("trang_thai")
    private InvoiceStatus status;

    @Indexed(unique = true, partialFilter = "{ 'ma_yeu_cau': { $type: 'string' } }")
    @Field("ma_yeu_cau")
    private String requestId;

    @Field("ghi_chu")
    private String note;

    @Field("ly_do_huy")
    private String cancelReason;

    @Field("id_nguoi_huy")
    private String cancelledById;

    @Field("ten_nguoi_huy")
    private String cancelledByName;

    @Field("ngay_huy")
    private Instant cancelledAt;

    @Indexed()
    @Field("ngay_tao")
    private Instant createdAt;
}
