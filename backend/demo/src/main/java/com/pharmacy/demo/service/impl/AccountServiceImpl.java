package com.pharmacy.demo.service.impl;

import com.pharmacy.demo.dto.request.ActivateAccountRequest;
import com.pharmacy.demo.dto.request.ResetPasswordRequest;
import com.pharmacy.demo.dto.response.ActivationInfoResponse;
import com.pharmacy.demo.entity.Employee;
import com.pharmacy.demo.entity.VerificationToken;
import com.pharmacy.demo.enums.EmployeeStatus;
import com.pharmacy.demo.enums.TokenType;
import com.pharmacy.demo.exception.InvalidTokenException;
import com.pharmacy.demo.repository.EmployeeRepository;
import com.pharmacy.demo.security.Actor;
import com.pharmacy.demo.service.AccountService;
import com.pharmacy.demo.service.ActivityLogger;
import com.pharmacy.demo.service.VerificationTokenService;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final EmployeeRepository employeeRepository;
    private final VerificationTokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final ActivityLogger activityLogger;
    private final Clock clock;

    @Override
    public ActivationInfoResponse describeActivation(String token) {
        VerificationToken found = tokenService.find(token, TokenType.ACTIVATION);
        Employee employee = employeeFor(found, EmployeeStatus.PENDING);
        return new ActivationInfoResponse(
                employee.getFullName(), employee.getUsername(), employee.getEmail(), employee.getRole());
    }

    @Override
    @Transactional
    public void activate(ActivateAccountRequest request) {
        VerificationToken consumed = tokenService.consume(request.token(), TokenType.ACTIVATION);
        Employee employee = employeeFor(consumed, EmployeeStatus.PENDING);
        applyPassword(employee, request.password());
        employee.setStatus(EmployeeStatus.ACTIVE);
        employeeRepository.save(employee);
        activityLogger.log(
                actorOf(employee),
                "ACTIVATE_ACCOUNT",
                "EMPLOYEE",
                employee.getId(),
                employee.getUsername(),
                employee.getFullName() + " kích hoạt tài khoản");
    }

    @Override
    public void validateResetToken(String token) {
        VerificationToken found = tokenService.find(token, TokenType.PASSWORD_RESET);
        employeeFor(found, null);
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        VerificationToken consumed = tokenService.consume(request.token(), TokenType.PASSWORD_RESET);
        Employee employee = employeeFor(consumed, null);
        applyPassword(employee, request.password());
        employeeRepository.save(employee);
        activityLogger.log(
                actorOf(employee),
                "RESET_PASSWORD",
                "EMPLOYEE",
                employee.getId(),
                employee.getUsername(),
                employee.getFullName() + " đặt lại mật khẩu");
    }

    private Employee employeeFor(VerificationToken token, EmployeeStatus requiredStatus) {
        Employee employee = employeeRepository.findById(token.getEmployeeId()).orElseThrow(InvalidTokenException::new);
        if (requiredStatus != null && employee.getStatus() != requiredStatus) {
            throw new InvalidTokenException();
        }
        if (requiredStatus == null && employee.getStatus() == EmployeeStatus.PENDING) {
            throw new InvalidTokenException();
        }
        return employee;
    }

    private void applyPassword(Employee employee, String password) {
        employee.setPasswordHash(passwordEncoder.encode(password));
        employee.setSecurityVersion(employee.getSecurityVersion() + 1);
        employee.setFailedLoginCount(0);
        employee.setLockedUntil(null);
        employee.setUpdatedAt(clock.instant());
    }

    private Actor actorOf(Employee employee) {
        return new Actor(employee.getId(), employee.getFullName(), employee.getRole());
    }
}
