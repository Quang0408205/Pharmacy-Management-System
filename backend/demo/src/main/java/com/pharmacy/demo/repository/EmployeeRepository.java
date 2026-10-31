package com.pharmacy.demo.repository;

import com.pharmacy.demo.entity.Employee;
import com.pharmacy.demo.enums.EmployeeStatus;
import com.pharmacy.demo.enums.Role;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface EmployeeRepository extends MongoRepository<Employee, String> {

    Optional<Employee> findByUsernameNormalized(String usernameNormalized);

    Optional<Employee> findByEmailNormalized(String emailNormalized);

    boolean existsByUsernameNormalized(String usernameNormalized);

    boolean existsByEmailNormalized(String emailNormalized);

    long countByRoleAndStatus(Role role, EmployeeStatus status);
}
