package organizationmanagement.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.util.AntPathMatcher;
import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import organizationmanagement.config.JwtTokenUtil;

@Component
@Slf4j
public class JwtRequestFilter extends OncePerRequestFilter {
    private final JwtTokenUtil jwtTokenUtil;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    // Public endpoints that don't require authentication
    private static final String[] PUBLIC_ENDPOINTS = {
            "/actuator/health",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/api/organizations", // for POST requests
            "/api/organizations/*/exists" // for GET requests
    };

    public JwtRequestFilter(JwtTokenUtil jwtTokenUtil) {
        this.jwtTokenUtil = jwtTokenUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // Skip JWT validation for public endpoints
        if (isPublicEndpoint(request)) {
            log.debug("Skipping JWT validation for public endpoint: {}", request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        final String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            log.warn("Missing or invalid Authorization header for request: {}", request.getRequestURI());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid Authorization header");
            return;
        }

        try {
            final String token = authorizationHeader.substring(7);
            log.debug("Validating JWT token for request: {}", request.getRequestURI());

            if (!jwtTokenUtil.isTokenValid(token)) {
                log.warn("Invalid or expired JWT token for request: {}", request.getRequestURI());
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired JWT token");
                return;
            }

            final String username = jwtTokenUtil.extractUsername(token);
            final UUID organizationId = jwtTokenUtil.extractOrganizationId(token);

            log.debug("JWT token validated successfully for user: {} in organization: {}", username, organizationId);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                List<String> authorities = jwtTokenUtil.extractAuthorities(token);
                List<GrantedAuthority> grantedAuthorities = authorities.stream()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                username,
                                null,
                                grantedAuthorities);

                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request));

                if (organizationId != null) {
                    request.setAttribute("organizationId", organizationId);
                }
                request.setAttribute("username", username);

                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("Authentication context set for user: {} with authorities: {}", username, authorities);
            }
        } catch (Exception e) {
            log.error("JWT token validation failed for request: {} - Error: {}", request.getRequestURI(), e.getMessage());
            SecurityContextHolder.clearContext();
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid JWT token: " + e.getMessage());
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isPublicEndpoint(HttpServletRequest request) {
        String requestPath = request.getRequestURI();
        String method = request.getMethod();


        for (String pattern : PUBLIC_ENDPOINTS) {
            if (pathMatcher.match(pattern, requestPath)) {
                // For /api/organizations, only allow POST requests
                if (pattern.equals("/api/organizations") && !"POST".equals(method)) {
                    continue;
                }
                // For /api/organizations/*/exists, only allow GET requests
                if (pattern.equals("/api/organizations/*/exists") && !"GET".equals(method)) {
                    continue;
                }
                return true;
            }
        }

        return false;
    }
}