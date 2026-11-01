package com.pharmacy.demo.service.impl;

import com.pharmacy.demo.dto.response.LoyaltyEntryResponse;
import com.pharmacy.demo.entity.Customer;
import com.pharmacy.demo.entity.Invoice;
import com.pharmacy.demo.entity.LoyaltyTransaction;
import com.pharmacy.demo.entity.embedded.LoyaltySetting;
import com.pharmacy.demo.enums.InvoiceStatus;
import com.pharmacy.demo.enums.LoyaltyType;
import com.pharmacy.demo.enums.MembershipTier;
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
import com.pharmacy.demo.service.TierStatus;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.TypedAggregation;
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
        int ratePercent = tierStatus(totalSpent(customer.getId())).earnRatePercent();
        return (int) (paid * ratePercent / (settings.getEarnAmountPerPoint() * 100));
    }

    @Override
    public TierStatus tierStatus(long totalSpent) {
        LoyaltySetting settings = settingService.current().getLoyalty();
        MembershipTier tier = MembershipTier.STANDARD;
        if (totalSpent >= settings.getDiamondThreshold()) {
            tier = MembershipTier.DIAMOND;
        } else if (totalSpent >= settings.getGoldThreshold()) {
            tier = MembershipTier.GOLD;
        } else if (totalSpent >= settings.getSilverThreshold()) {
            tier = MembershipTier.SILVER;
        }
        MembershipTier next = switch (tier) {
            case STANDARD -> MembershipTier.SILVER;
            case SILVER -> MembershipTier.GOLD;
            case GOLD -> MembershipTier.DIAMOND;
            case DIAMOND -> null;
        };
        long needed = next == null ? 0 : Math.max(0, thresholdOf(settings, next) - totalSpent);
        return new TierStatus(tier, next, needed, rateOf(settings, tier));
    }

    @Override
    public long totalSpent(String customerId) {
        TypedAggregation<Invoice> aggregation = Aggregation.newAggregation(
                Invoice.class,
                Aggregation.match(Criteria.where("status")
                        .is(InvoiceStatus.COMPLETED)
                        .and("customerId")
                        .is(customerId)),
                Aggregation.group().sum("total").as("total"));
        Document row = mongoTemplate.aggregate(aggregation, Document.class).getUniqueMappedResult();
        return row == null ? 0 : row.get("total", Number.class).longValue();
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

    private static long thresholdOf(LoyaltySetting settings, MembershipTier tier) {
        return switch (tier) {
            case STANDARD -> 0;
            case SILVER -> settings.getSilverThreshold();
            case GOLD -> settings.getGoldThreshold();
            case DIAMOND -> settings.getDiamondThreshold();
        };
    }

    private static int rateOf(LoyaltySetting settings, MembershipTier tier) {
        return switch (tier) {
            case STANDARD -> 100;
            case SILVER -> settings.getSilverRate();
            case GOLD -> settings.getGoldRate();
            case DIAMOND -> settings.getDiamondRate();
        };
    }

    private static BadRequestException reject(String message) {
        return new BadRequestException(message, Map.of("pointsUsed", message));
    }
}
