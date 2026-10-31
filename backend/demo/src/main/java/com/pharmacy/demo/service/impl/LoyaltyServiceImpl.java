package com.pharmacy.demo.service.impl;

import com.pharmacy.demo.dto.response.LoyaltyEntryResponse;
import com.pharmacy.demo.entity.Customer;
import com.pharmacy.demo.entity.Invoice;
import com.pharmacy.demo.entity.LoyaltyTransaction;
import com.pharmacy.demo.entity.embedded.LoyaltySetting;
import com.pharmacy.demo.enums.LoyaltyType;
import com.pharmacy.demo.exception.BadRequestException;
import com.pharmacy.demo.exception.ConflictException;
import com.pharmacy.demo.exception.NotFoundException;
import com.pharmacy.demo.mapper.LoyaltyMapper;
import com.pharmacy.demo.repository.CustomerRepository;
import com.pharmacy.demo.repository.LoyaltyTransactionRepository;
import com.pharmacy.demo.security.Actor;
import com.pharmacy.demo.service.IdGenerator;
import com.pharmacy.demo.service.LoyaltyService;
import com.pharmacy.demo.service.SystemSettingService;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoyaltyServiceImpl implements LoyaltyService {

    private static final String POINTS_FIELD = "loyaltyPoints";

    private final SystemSettingService settingService;
    private final CustomerRepository customerRepository;
    private final LoyaltyTransactionRepository transactionRepository;
    private final LoyaltyMapper loyaltyMapper;
    private final IdGenerator idGenerator;
    private final MongoTemplate mongoTemplate;
    private final Clock clock;

    @Override
    public LoyaltySetting settings() {
        return settingService.current().getLoyalty();
    }

    @Override
    public long redeemValue(Customer customer, int points, long payable) {
        if (points <= 0) {
            return 0;
        }
        LoyaltySetting settings = settings();
        if (customer == null) {
            throw reject("Cần chọn khách hàng để sử dụng điểm.");
        }
        if (!settings.isEnabled()) {
            throw reject("Chương trình tích điểm đang tắt.");
        }
        if (points > customer.getLoyaltyPoints()) {
            throw reject("Khách hàng chỉ còn " + customer.getLoyaltyPoints() + " điểm.");
        }
        long maxPoints = payable * settings.getMaxRedeemPercent() / 100 / settings.getPointValue();
        if (points > maxPoints) {
            throw reject("Đơn hàng này chỉ được dùng tối đa " + maxPoints + " điểm.");
        }
        return points * settings.getPointValue();
    }

    @Override
    public int earnedPoints(Customer customer, long paid) {
        LoyaltySetting settings = settings();
        if (customer == null || !settings.isEnabled() || paid <= 0) {
            return 0;
        }
        return (int) (paid / settings.getEarnAmountPerPoint());
    }

    @Override
    public void settle(Invoice invoice, Actor actor) {
        if (invoice.getCustomerId() == null) {
            return;
        }
        if (invoice.getPointsUsed() > 0) {
            Customer updated = adjust(invoice.getCustomerId(), -invoice.getPointsUsed(), true);
            if (updated == null) {
                throw new ConflictException("Khách hàng không đủ điểm để thanh toán.");
            }
            record(invoice, LoyaltyType.REDEEM, -invoice.getPointsUsed(), updated, actor, "Dùng điểm thanh toán");
        }
        if (invoice.getPointsEarned() > 0) {
            Customer updated = adjust(invoice.getCustomerId(), invoice.getPointsEarned(), false);
            record(invoice, LoyaltyType.EARN, invoice.getPointsEarned(), updated, actor, "Tích điểm từ hóa đơn");
        }
    }

    @Override
    public void reverse(Invoice invoice, Actor actor) {
        if (invoice.getCustomerId() == null) {
            return;
        }
        if (invoice.getPointsUsed() > 0) {
            Customer updated = adjust(invoice.getCustomerId(), invoice.getPointsUsed(), false);
            record(
                    invoice,
                    LoyaltyType.REDEEM_REFUND,
                    invoice.getPointsUsed(),
                    updated,
                    actor,
                    "Hoàn điểm do hủy hóa đơn");
        }
        if (invoice.getPointsEarned() > 0) {
            revokeEarned(invoice, actor);
        }
    }

    @Override
    public List<LoyaltyEntryResponse> history(String customerId) {
        if (!customerRepository.existsById(customerId)) {
            throw new NotFoundException("Không tìm thấy khách hàng.");
        }
        return transactionRepository.findTop50ByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(loyaltyMapper::toResponse)
                .toList();
    }

    private void revokeEarned(Invoice invoice, Actor actor) {
        Customer customer = customerRepository.findById(invoice.getCustomerId()).orElse(null);
        if (customer == null) {
            return;
        }
        int revoked = Math.min(invoice.getPointsEarned(), customer.getLoyaltyPoints());
        if (revoked <= 0) {
            return;
        }
        Customer updated = adjust(invoice.getCustomerId(), -revoked, true);
        if (updated == null) {
            throw new ConflictException("Điểm của khách hàng vừa thay đổi, vui lòng thử lại.");
        }
        String note = revoked < invoice.getPointsEarned()
                ? "Thu hồi điểm do hủy hóa đơn (chỉ thu hồi " + revoked + "/" + invoice.getPointsEarned()
                        + " điểm vì khách đã dùng)"
                : "Thu hồi điểm do hủy hóa đơn";
        record(invoice, LoyaltyType.EARN_REVOKE, -revoked, updated, actor, note);
    }

    private Customer adjust(String customerId, int delta, boolean requireBalance) {
        Criteria criteria = Criteria.where("_id").is(customerId);
        if (requireBalance && delta < 0) {
            criteria = criteria.and(POINTS_FIELD).gte(-delta);
        }
        return mongoTemplate.findAndModify(
                Query.query(criteria),
                new Update().inc(POINTS_FIELD, delta),
                FindAndModifyOptions.options().returnNew(true),
                Customer.class);
    }

    private void record(Invoice invoice, LoyaltyType type, int points, Customer updated, Actor actor, String note) {
        transactionRepository.insert(LoyaltyTransaction.builder()
                .id(idGenerator.newId())
                .customerId(updated.getId())
                .type(type)
                .points(points)
                .balanceAfter(updated.getLoyaltyPoints())
                .invoiceId(invoice.getId())
                .invoiceCode(invoice.getCode())
                .note(note)
                .actorId(actor.id())
                .actorName(actor.name())
                .createdAt(clock.instant())
                .build());
    }

    private static BadRequestException reject(String message) {
        return new BadRequestException(message, Map.of("pointsUsed", message));
    }
}
