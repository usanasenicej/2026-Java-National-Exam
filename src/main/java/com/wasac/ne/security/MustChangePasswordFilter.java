package com.wasac.ne.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wasac.ne.exception.ErrorResponse;
import com.wasac.ne.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class MustChangePasswordFilter extends OncePerRequestFilter {

    /**
     * Paths that are always allowed even when mustChangePassword = true.
     * Keep this list tight — only the password-change and session endpoints.
     */
    private static final Set<String> ALWAYS_ALLOWED = Set.of(
            "/api/auth/change-password",
            "/api/auth/logout",
            "/api/auth/me",
            "/api/auth/refresh-token"
    );

    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Skip entirely for public / infrastructure paths — no DB hit needed
        return path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/actuator")
                || path.startsWith("/api/auth/register")
                || path.startsWith("/api/auth/login")
                || path.startsWith("/api/auth/verify")
                || path.startsWith("/api/auth/resend-otp")
                || path.startsWith("/api/auth/forgot-password")
                || path.startsWith("/api/auth/reset-password")
                || path.startsWith("/api/auth/refresh-token")
                || path.startsWith("/error");
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String path = request.getRequestURI();

        // Only enforce for fully-authenticated users (not anonymous)
        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            filterChain.doFilter(request, response);
            return;
        }

        // Allow change-password and logout always
        if (ALWAYS_ALLOWED.contains(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Check mustChangePassword flag — single DB call, result is already in JPA first-level cache
        try {
            boolean mustChange = userRepository.findByEmail(authentication.getName())
                    .map(u -> u.isMustChangePassword())
                    .orElse(false);

            if (mustChange) {
                writeForbiddenResponse(response, path);
                return;
            }
        } catch (Exception e) {
            // If we can't check, let the request through — don't block on infrastructure errors
            logger.warn("MustChangePasswordFilter: could not load user " + authentication.getName() + ": " + e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private void writeForbiddenResponse(HttpServletResponse response, String path) throws IOException {
        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpServletResponse.SC_FORBIDDEN)
                .error("Password Change Required")
                .message("You must change your temporary password before accessing the system. "
                        + "Call POST /api/auth/change-password with your current and new password.")
                .path(path)
                .build();

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
