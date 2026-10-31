package com.pharmacy.demo.dto.response;

import com.pharmacy.demo.enums.DiscountType;
import com.pharmacy.demo.enums.InvoiceStatus;
import com.pharmacy.demo.enums.PaymentMethod;
import com.pharmacy.demo.enums.Role;
import java.time.Instant;
import java.util.List;

public record InvoiceDetailResponse(
        String id,
        String code,
        String employeeId,
        String employeeName,
        String customerId,
        String customerName,
        List<InvoiceLineResponse> items,
        Long subtotal,
        DiscountType discountType,
        Long discountValue,
        Long discountAmount,
        int pointsUsed,
        long pointsDiscount,
        int pointsEarned,
        Long total,
        PaymentMethod paymentMethod,
        Long receivedAmount,
        Long changeAmount,
        boolean prescriptionConfirmed,
        String prescriptionCode,
        InvoiceStatus status,
        String note,
        String cancelReason,
        String cancelledByName,
        Instant cancelledAt,
        Instant createdAt,
        CustomerRef customer,
        EmployeeRef employee,
        SettingsResponse.Pharmacy pharmacy) {

    public static InvoiceDetailResponse withParties(
            InvoiceDetailResponse base,
            CustomerRef customer,
            EmployeeRef employee,
            SettingsResponse.Pharmacy pharmacy) {
        return new InvoiceDetailResponse(
                base.id,
                base.code,
                base.employeeId,
                base.employeeName,
                base.customerId,
                base.customerName,
                base.items,
                base.subtotal,
                base.discountType,
                base.discountValue,
                base.discountAmount,
                base.pointsUsed,
                base.pointsDiscount,
                base.pointsEarned,
                base.total,
                base.paymentMethod,
                base.receivedAmount,
                base.changeAmount,
                base.prescriptionConfirmed,
                base.prescriptionCode,
                base.status,
                base.note,
                base.cancelReason,
                base.cancelledByName,
                base.cancelledAt,
                base.createdAt,
                customer,
                employee,
                pharmacy);
    }

    public record CustomerRef(String id, String code, String name, String phone) {}

    public record EmployeeRef(String id, String name, Role role, String email) {}
}
