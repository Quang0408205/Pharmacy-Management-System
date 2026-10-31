package com.pharmacy.demo.service.impl;

import com.pharmacy.demo.config.AppProperties;
import com.pharmacy.demo.dto.common.ListPager;
import com.pharmacy.demo.dto.common.PageQuery;
import com.pharmacy.demo.dto.common.PageResponse;
import com.pharmacy.demo.dto.request.CreateEmployeeRequest;
import com.pharmacy.demo.dto.response.ActivityResponse;
import com.pharmacy.demo.dto.response.EmployeeCreatedResponse;
import com.pharmacy.demo.dto.response.EmployeeDetailResponse;
import com.pharmacy.demo.dto.response.EmployeeResponse;
import com.pharmacy.demo.dto.response.ResetLinkResponse;
import com.pharmacy.demo.entity.Employee;
import com.pharmacy.demo.entity.Invoice;
import com.pharmacy.demo.enums.EmployeeStatus;
import com.pharmacy.demo.enums.InvoiceStatus;
import com.pharmacy.demo.enums.ReceiptStatus;
import com.pharmacy.demo.enums.Role;
import com.pharmacy.demo.enums.TokenType;
import com.pharmacy.demo.exception.BadRequestException;
import com.pharmacy.demo.exception.ConflictException;
import com.pharmacy.demo.exception.NotFoundException;
import com.pharmacy.demo.mapper.ActivityLogMapper;
import com.pharmacy.demo.repository.ActivityLogRepository;
import com.pharmacy.demo.repository.EmployeeRepository;
import com.pharmacy.demo.repository.GoodsReceiptRepository;
import com.pharmacy.demo.security.Actor;
import com.pharmacy.demo.service.ActivityLogger;
import com.pharmacy.demo.service.CodeGenerator;
import com.pharmacy.demo.service.CodeKind;
import com.pharmacy.demo.service.EmployeeService;
import com.pharmacy.demo.service.IdGenerator;
import com.pharmacy.demo.service.VerificationTokenService;
import com.pharmacy.demo.service.VerificationTokenService.IssuedToken;
import com.pharmacy.demo.util.AccountLinks;
import com.pharmacy.demo.util.TextSearch;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.TypedAggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private static final String NOT_FOUND_MESSAGE = "Không tìm thấy nhân viên.";
    private static final String LAST_ADMIN_MESSAGE = "Hệ thống phải có ít nhất một quản trị viên đang hoạt động.";
    private static final Map<String, Comparator<EmployeeResponse>> SORTABLE = Map.of(
            "name", ListPager.text(EmployeeResponse::name),
            "email", ListPager.text(EmployeeResponse::email),
            "role", ListPager.comparable(EmployeeResponse::role),
            "status", ListPager.comparable(EmployeeResponse::status),
            "lastLoginAt", ListPager.comparable(EmployeeResponse::lastLoginAt),
            "invoiceCount", ListPager.comparable(EmployeeResponse::invoiceCount));

    private final EmployeeRepository employeeRepository;
    private final GoodsReceiptRepository goodsReceiptRepository;
    private final ActivityLogRepository activityLogRepository;
    private final ActivityLogMapper activityLogMapper;
    private final VerificationTokenService tokenService;
    private final CodeGenerator codeGenerator;
    private final ActivityLogger activityLogger;
    private final IdGenerator idGenerator;
    private final MongoTemplate mongoTemplate;
    private final AppProperties properties;
    private final Clock clock;

    @Override
    public PageResponse<EmployeeResponse> search(EmployeeFilter filter, PageQuery query) {
        Instant now = clock.instant();
        Map<String, InvoiceTotals> totals = invoiceTotals();
        List<EmployeeResponse> items = employeeRepository.findAll().stream()
                .filter(employee -> filter.role() == null || employee.getRole() == filter.role())
                .filter(employee -> filter.status() == null || employee.getStatus() == filter.status())
                .filter(employee -> TextSearch.matchesAny(
                        filter.q(),
                        employee.getFullName(),
                        employee.getEmail(),
                        employee.getUsername(),
                        employee.getPhone(),
                        employee.getCode()))
                .map(employee -> toResponse(employee, totals.getOrDefault(employee.getId(), InvoiceTotals.EMPTY), now))
                .toList();
        return ListPager.paginate(items, query, SORTABLE, "name,asc");
    }

    @Override
    public EmployeeDetailResponse get(String id) {
        Employee employee = find(id);
        InvoiceTotals totals = invoiceTotals().getOrDefault(id, InvoiceTotals.EMPTY);
        EmployeeResponse summary = toResponse(employee, totals, clock.instant());
        List<ActivityResponse> activities = activityLogRepository.findTop8ByActorIdOrderByCreatedAtDesc(id).stream()
                .map(activityLogMapper::toResponse)
                .toList();
        return new EmployeeDetailResponse(
                summary.id(),
                summary.code(),
                summary.name(),
                summary.email(),
                summary.username(),
                summary.phone(),
                summary.role(),
                summary.status(),
                summary.locked(),
                summary.lockedUntil(),
                summary.createdAt(),
                summary.lastLoginAt(),
                totals.count(),
                totals.revenue(),
                goodsReceiptRepository.countByCreatedByIdAndStatus(id, ReceiptStatus.CONFIRMED),
                activities);
    }

    @Override
    @Transactional
    public EmployeeCreatedResponse create(CreateEmployeeRequest request, Actor actor) {
        String username = request.username().trim();
        String email = request.email().trim();
        Map<String, String> conflicts = new HashMap<>();
        if (employeeRepository.existsByUsernameNormalized(username.toLowerCase(Locale.ROOT))) {
            conflicts.put("username", "Tên đăng nhập đã được sử dụng.");
        }
        if (employeeRepository.existsByEmailNormalized(email.toLowerCase(Locale.ROOT))) {
            conflicts.put("email", "Email đã có tài khoản.");
        }
        if (!conflicts.isEmpty()) {
            throw new ConflictException("Thông tin nhân viên bị trùng.", conflicts);
        }
        Instant now = clock.instant();
        Employee employee = employeeRepository.save(Employee.builder()
                .id(idGenerator.newId())
                .code(codeGenerator.next(CodeKind.EMPLOYEE))
                .username(username)
                .usernameNormalized(username.toLowerCase(Locale.ROOT))
                .fullName(request.name().trim())
                .email(email)
                .emailNormalized(email.toLowerCase(Locale.ROOT))
                .phone(blankToNull(request.phone()))
                .role(request.role())
                .status(EmployeeStatus.PENDING)
                .securityVersion(1L)
                .createdById(actor.id())
                .createdAt(now)
                .updatedAt(now)
                .build());
        IssuedToken token = tokenService.issue(
                employee.getId(), TokenType.ACTIVATION, properties.security().activationTtl(), actor);
        activityLogger.log(
                actor,
                "CREATE_EMPLOYEE",
                "EMPLOYEE",
                employee.getId(),
                employee.getUsername(),
                "Tạo tài khoản " + employee.getFullName() + " với vai trò " + employee.getRole());
        return new EmployeeCreatedResponse(
                toResponse(employee, InvoiceTotals.EMPTY, now),
                AccountLinks.activation(token.value()),
                token.expiresAt());
    }

    @Override
    @Transactional
    public EmployeeResponse changeRole(String id, Role role, Actor actor) {
        Employee employee = find(id);
        if (employee.getRole() == role) {
            return respond(employee);
        }
        if (employee.getRole() == Role.ADMIN) {
            requireAnotherActiveAdmin(employee);
            if (employee.getId().equals(actor.id())) {
                throw new ConflictException("Bạn không thể tự hạ quyền của chính mình.");
            }
        }
        employee.setRole(role);
        employee.setSecurityVersion(employee.getSecurityVersion() + 1);
        employee.setUpdatedAt(clock.instant());
        employeeRepository.save(employee);
        activityLogger.log(
                actor,
                "ASSIGN_ROLE",
                "EMPLOYEE",
                employee.getId(),
                employee.getUsername(),
                "Gán vai trò " + role + " cho " + employee.getFullName());
        return respond(employee);
    }

    @Override
    @Transactional
    public EmployeeResponse changeStatus(String id, EmployeeStatus status, Actor actor) {
        if (status == EmployeeStatus.PENDING) {
            throw new BadRequestException("Trạng thái không hợp lệ.", Map.of("status", "Trạng thái không hợp lệ."));
        }
        Employee employee = find(id);
        if (employee.getStatus() == status) {
            return respond(employee);
        }
        if (status == EmployeeStatus.INACTIVE) {
            if (employee.getId().equals(actor.id())) {
                throw new ConflictException("Bạn không thể vô hiệu hóa chính tài khoản của mình.");
            }
            if (employee.getRole() == Role.ADMIN) {
                requireAnotherActiveAdmin(employee);
            }
        } else if (employee.getPasswordHash() == null) {
            throw new ConflictException("Nhân viên chưa đặt mật khẩu. Hãy tạo link kích hoạt cho nhân viên này.");
        }
        employee.setStatus(status);
        employee.setSecurityVersion(employee.getSecurityVersion() + 1);
        employee.setUpdatedAt(clock.instant());
        employeeRepository.save(employee);
        activityLogger.log(
                actor,
                "UPDATE_EMPLOYEE_STATUS",
                "EMPLOYEE",
                employee.getId(),
                employee.getUsername(),
                (status == EmployeeStatus.ACTIVE ? "Kích hoạt" : "Vô hiệu hóa") + " tài khoản "
                        + employee.getFullName());
        return respond(employee);
    }

    @Override
    @Transactional
    public ResetLinkResponse issueResetLink(String id, Actor actor) {
        Employee employee = find(id);
        boolean pending = employee.getStatus() == EmployeeStatus.PENDING;
        TokenType type = pending ? TokenType.ACTIVATION : TokenType.PASSWORD_RESET;
        IssuedToken token = tokenService.issue(
                employee.getId(),
                type,
                pending
                        ? properties.security().activationTtl()
                        : properties.security().resetTtl(),
                actor);
        activityLogger.log(
                actor,
                "CREATE_RESET_LINK",
                "EMPLOYEE",
                employee.getId(),
                employee.getUsername(),
                (pending ? "Tạo lại link kích hoạt cho " : "Tạo link đặt lại mật khẩu cho ") + employee.getFullName());
        String link = pending ? AccountLinks.activation(token.value()) : AccountLinks.reset(token.value());
        return new ResetLinkResponse(link, token.expiresAt());
    }

    @Override
    @Transactional
    public EmployeeResponse unlock(String id, Actor actor) {
        Employee employee = find(id);
        employee.setFailedLoginCount(0);
        employee.setLockedUntil(null);
        employee.setUpdatedAt(clock.instant());
        employeeRepository.save(employee);
        activityLogger.log(
                actor,
                "UNLOCK_EMPLOYEE",
                "EMPLOYEE",
                employee.getId(),
                employee.getUsername(),
                "Mở khóa tài khoản " + employee.getFullName());
        return respond(employee);
    }

    private Employee find(String id) {
        return employeeRepository.findById(id).orElseThrow(() -> new NotFoundException(NOT_FOUND_MESSAGE));
    }

    private void requireAnotherActiveAdmin(Employee employee) {
        boolean countsAsActiveAdmin = employee.getStatus() == EmployeeStatus.ACTIVE;
        long activeAdmins = employeeRepository.countByRoleAndStatus(Role.ADMIN, EmployeeStatus.ACTIVE);
        if (countsAsActiveAdmin && activeAdmins <= 1) {
            throw new ConflictException(LAST_ADMIN_MESSAGE);
        }
    }

    private EmployeeResponse respond(Employee employee) {
        InvoiceTotals totals = invoiceTotals().getOrDefault(employee.getId(), InvoiceTotals.EMPTY);
        return toResponse(employee, totals, clock.instant());
    }

    private EmployeeResponse toResponse(Employee employee, InvoiceTotals totals, Instant now) {
        boolean locked =
                employee.getLockedUntil() != null && employee.getLockedUntil().isAfter(now);
        return new EmployeeResponse(
                employee.getId(),
                employee.getCode(),
                employee.getFullName(),
                employee.getEmail(),
                employee.getUsername(),
                employee.getPhone(),
                employee.getRole(),
                employee.getStatus(),
                locked,
                locked ? employee.getLockedUntil() : null,
                employee.getCreatedAt(),
                employee.getLastLoginAt(),
                totals.count());
    }

    private Map<String, InvoiceTotals> invoiceTotals() {
        TypedAggregation<Invoice> aggregation = Aggregation.newAggregation(
                Invoice.class,
                Aggregation.match(Criteria.where("status").is(InvoiceStatus.COMPLETED)),
                Aggregation.group("employeeId").count().as("count").sum("total").as("revenue"));
        Map<String, InvoiceTotals> totals = new HashMap<>();
        for (Document row : mongoTemplate.aggregate(aggregation, Document.class).getMappedResults()) {
            totals.put(
                    row.getString("_id"),
                    new InvoiceTotals(
                            row.get("count", Number.class).longValue(),
                            row.get("revenue", Number.class).longValue()));
        }
        return totals;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record InvoiceTotals(long count, long revenue) {

        static final InvoiceTotals EMPTY = new InvoiceTotals(0, 0);
    }
}
