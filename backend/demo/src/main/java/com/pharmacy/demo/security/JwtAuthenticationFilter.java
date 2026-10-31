package com.pharmacy.demo.security;

import com.pharmacy.demo.enums.EmployeeStatus;
import com.pharmacy.demo.repository.EmployeeRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AuthCookieService cookieService;
    private final EmployeeRepository employeeRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            cookieService
                    .read(request)
                    .flatMap(jwtService::parse)
                    .flatMap(claims -> employeeRepository
                            .findById(claims.subject())
                            .filter(employee -> employee.getStatus() == EmployeeStatus.ACTIVE)
                            .filter(employee -> employee.getSecurityVersion() == claims.securityVersion()))
                    .ifPresent(employee -> {
                        Actor actor = new Actor(employee.getId(), employee.getFullName(), employee.getRole());
                        SecurityContextHolder.getContext()
                                .setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                                        actor,
                                        null,
                                        List.of(new SimpleGrantedAuthority(
                                                "ROLE_" + employee.getRole().name()))));
                    });
        }
        filterChain.doFilter(request, response);
    }
}
