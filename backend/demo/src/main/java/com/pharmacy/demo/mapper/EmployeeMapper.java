package com.pharmacy.demo.mapper;

import com.pharmacy.demo.dto.response.UserResponse;
import com.pharmacy.demo.entity.Employee;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface EmployeeMapper {

    @Mapping(target = "name", source = "fullName")
    UserResponse toUserResponse(Employee employee);
}
