package com.pharmacy.demo.service.impl;

import com.pharmacy.demo.config.AppProperties;
import com.pharmacy.demo.dto.request.LoginRequest;
import com.pharmacy.demo.dto.response.UserResponse;
import com.pharmacy.demo.entity.Employee;
import com.pharmacy.demo.enums.EmployeeStatus;
import com.pharmacy.demo.exception.ForbiddenException;
import com.pharmacy.demo.exception.UnauthorizedException;
import com.pharmacy.demo.mapper.EmployeeMapper;
import com.pharmacy.demo.repository.EmployeeRepository;
import com.pharmacy.demo.security.Actor;
import com.pharmacy.demo.security.JwtService;
import com.pharmacy.demo.service.ActivityLogger;
import com.pharmacy.demo.service.AuthService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private static final String INVALID_CREDENTIALS_CODE = "INVALID_CREDENTIALS";
    private static final String INVALID_CREDENTIALS_MESSAGE = "Email/tên đăng nhập hoặc mật khẩu không đúng.";
    private static final DateTimeFormatter LOCK_TIME = DateTimeFormatter.ofPattern("HH:mm 'ngày' dd/MM/yyyy");

    private final EmployeeRepository employeeRepository;
    private final MongoTemplate mongoTemplate;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmployeeMapper employeeMapper;
    private final ActivityLogger activityLogger;
    private final AppProperties properties;
    private final Clock clock;
    private final String dummyHash;

    public AuthServiceImpl(
            EmployeeRepository employeeRepository,
            MongoTemplate mongoTemplate,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            EmployeeMapper employeeMapper,
            ActivityLogger activityLogger,
            AppProperties properties,
            Clock clock) {
        this.employeeRepository = employeeRepository;
        this.mongoTemplate = mongoTemplate;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.employeeMapper = employeeMapper;
        this.activityLogger = activityLogger;
        this.properties = properties;
        this.clock = clock;
        this.dummyHash = passwordEncoder.encode("not-a-real-password");
    }

    @Override
    public LoginResult login(LoginRequest request) {
        String identifier = request.identifier().trim().toLowerCase(Locale.ROOT);
        Optional<Employee> found = employeeRepository
                .findByUsernameNormalized(identifier)
                .or(() -> employeeRepository.findByEmailNormalized(identifier));
        if (found.isEmpty()) {
            passwordEncoder.matches(request.password(), dummyHash);
            throw invalidCredentials();
        }
        Employee employee = found.get();
        Instant now = clock.instant();
        if (employee.getLockedUntil() != null && employee.getLockedUntil().isAfter(now)) {
            throw lockedOut(employee.getLockedUntil());
        }
        if (employee.getPasswordHash() == null
                || !passwordEncoder.matches(request.password(), employee.getPasswordHash())) {
            registerFailure(employee, now);
            throw invalidCredentials();
        }
        if (employee.getStatus() == EmployeeStatus.PENDING) {
            throw new ForbiddenException("ACCOUNT_PENDING", "Tài khoản chưa được kích hoạt.");
        }
        if (employee.getStatus() != EmployeeStatus.ACTIVE) {
            throw new ForbiddenException(
                    "ACCOUNT_DISABLED", "Tài khoản đã bị vô hiệu hóa. Vui lòng liên hệ quản trị viên.");
        }
        Employee updated = markLoggedIn(employee, now);
        Duration ttl = Boolean.TRUE.equals(request.remember())
                ? properties.security().rememberTtl()
                : properties.security().sessionTtl();
        activityLogger.log(
                new Actor(updated.getId(), updated.getFullName(), updated.getRole()),
                "LOGIN",
                "AUTH",
                updated.getId(),
                updated.getUsername(),
                updated.getFullName() + " đăng nhập hệ thống");
        return new LoginResult(employeeMapper.toUserResponse(updated), jwtService.issue(updated, ttl), ttl);
    }

    @Override
    public UserResponse currentUser(String employeeId) {
        return employeeRepository
                .findById(employeeId)
                .filter(employee -> employee.getStatus() == EmployeeStatus.ACTIVE)
                .map(employeeMapper::toUserResponse)
                .orElseThrow(UnauthorizedException::new);
    }

    private void registerFailure(Employee employee, Instant now) {
        Employee updated = mongoTemplate.findAndModify(
                Query.query(Criteria.where("_id").is(employee.getId())),
                new Update().inc("failedLoginCount", 1),
                FindAndModifyOptions.options().returnNew(true),
                Employee.class);
        if (updated != null
                && updated.getFailedLoginCount() >= properties.security().maxFailedLogins()) {
            Instant lockedUntil = now.plus(properties.security().lockDuration());
            mongoTemplate.updateFirst(
                    Query.query(Criteria.where("_id").is(employee.getId())),
                    new Update().set("failedLoginCount", 0).set("lockedUntil", lockedUntil),
                    Employee.class);
            activityLogger.log(
                    Actor.system(),
                    "LOGIN_FAILED_LOCKED",
                    "AUTH",
                    employee.getId(),
                    employee.getUsername(),
                    "Khóa tạm tài khoản " + employee.getUsername() + " do đăng nhập sai nhiều lần");
        }
    }

    private Employee markLoggedIn(Employee employee, Instant now) {
        return mongoTemplate.findAndModify(
                Query.query(Criteria.where("_id").is(employee.getId())),
                new Update().set("failedLoginCount", 0).set("lockedUntil", null).set("lastLoginAt", now),
                FindAndModifyOptions.options().returnNew(true),
                Employee.class);
    }

    private UnauthorizedException invalidCredentials() {
        return new UnauthorizedException(INVALID_CREDENTIALS_CODE, INVALID_CREDENTIALS_MESSAGE);
    }

    private ForbiddenException lockedOut(Instant lockedUntil) {
        String until = LOCK_TIME.format(ZonedDateTime.ofInstant(lockedUntil, clock.getZone()));
        return new ForbiddenException(
                "ACCOUNT_LOCKED", "Tài khoản tạm thời bị khóa do đăng nhập sai nhiều lần. Thử lại sau " + until + ".");
    }
}
