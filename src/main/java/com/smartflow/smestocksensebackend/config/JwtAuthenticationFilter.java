package com.smartflow.smestocksensebackend.config;

import com.smartflow.smestocksensebackend.entity.Employee;
import com.smartflow.smestocksensebackend.repository.EmployeeRepository;
import com.smartflow.smestocksensebackend.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final EmployeeRepository employeeRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String authorizationHeader = request.getHeader("Authorization");
        log.debug("JwtAuthenticationFilter request uri={} authorizationHeader={}", request.getRequestURI(),
                authorizationHeader);
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            log.debug("JwtAuthenticationFilter skipped: no Bearer Authorization header");
            filterChain.doFilter(request, response);
            return;
        }

        String token = authorizationHeader.substring(BEARER_PREFIX.length());
        boolean tokenValid = jwtService.isTokenValid(token);
        log.debug("JwtAuthenticationFilter token valid={}", tokenValid);

        if (SecurityContextHolder.getContext().getAuthentication() == null && tokenValid) {
            jwtService.extractSubject(token)
                    .flatMap(employeeRepository::findByEmailIgnoreCase)
                    .ifPresentOrElse(employee -> {
                        log.debug("JwtAuthenticationFilter token subject={}", employee.getEmail());
                        authenticate(request, employee);
                    }, () -> log.debug("JwtAuthenticationFilter token subject did not resolve to employee"));
        }

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        log.debug("JwtAuthenticationFilter after auth authentication={}", authentication);
        if (authentication != null) {
            log.debug("JwtAuthenticationFilter auth details: authenticated={}, principal={}, authorities={}",
                    authentication.isAuthenticated(), authentication.getPrincipal(), authentication.getAuthorities());
        }

        filterChain.doFilter(request, response);
    }

    private void authenticate(HttpServletRequest request, Employee employee) {
        if (employee.getRole() == null || employee.getRole().getCode() == null) {
            return;
        }

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                employee,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + employee.getRole().getCode().name())));
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
