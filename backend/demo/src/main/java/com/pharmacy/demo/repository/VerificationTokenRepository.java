package com.pharmacy.demo.repository;

import com.pharmacy.demo.entity.VerificationToken;
import com.pharmacy.demo.enums.TokenType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface VerificationTokenRepository extends MongoRepository<VerificationToken, String> {

    Optional<VerificationToken> findByTokenHash(String tokenHash);

    List<VerificationToken> findByEmployeeIdAndTypeAndUsedAtIsNull(String employeeId, TokenType type);
}
