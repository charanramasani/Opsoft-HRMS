
package com.opsoft.hrms_backend.security;

import java.io.IOException;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.opsoft.hrms_backend.entity.Employee;
import com.opsoft.hrms_backend.repository.EmployeeRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class FirstLoginPasswordFilter extends OncePerRequestFilter {

    private final EmployeeRepository employeeRepository;

    public FirstLoginPasswordFilter(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {

        String path = request.getServletPath();

        return "OPTIONS".equalsIgnoreCase(request.getMethod())
                || "/api/auth/login".equals(path)
                || "/api/auth/change-password".equals(path)
                || "/api/auth/logout".equals(path)
                || "/api/test".equals(path)
                || "/api/setup/initial-admin".equals(path);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {

            String employeeId = authentication.getName();

            Employee employee =
                    employeeRepository.findByEmployeeId(employeeId);

            if (employee != null && employee.isMustChangePassword()) {

                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");

                response.getWriter().write(
                        "{\"message\":\"Please change your temporary password before accessing other modules.\"}");

                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
