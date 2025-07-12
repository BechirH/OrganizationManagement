package organizationmanagement.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.util.AntPathMatcher;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class GatewayAuthenticationFilter extends OncePerRequestFilter {
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    // Public endpoints that don't require authentication
    private static final String[] PUBLIC_ENDPOINTS = {
            "/actuator/health",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/api/organizations", // for POST requests
            "/api/organizations/*/exists", // for GET requests
            "/api/departments/*/exists", // for GET requests
            "/api/teams/*/exists", // for GET requests
            "/api/departments/user/**", // for GET requests
            "/api/teams/user/**" // for GET requests
    };

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // Skip authentication for public endpoints
        if (isPublicEndpoint(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Check if request is authenticated by gateway
        String authenticated = request.getHeader("X-Authenticated");
        if (!"true".equals(authenticated)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Request not authenticated by gateway");
            return;
        }

        // Extract user context from Gateway headers
        String username = request.getHeader("X-User-Name");
        String organizationIdStr = request.getHeader("X-Organization-Id");
        String authoritiesStr = request.getHeader("X-User-Authorities");

        if (username == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing user context from Gateway");
            return;
        }

        try {
            // Parse organization ID if present
            UUID organizationId = null;
            if (organizationIdStr != null && !organizationIdStr.isEmpty()) {
                organizationId = UUID.fromString(organizationIdStr);
            }

            // Parse authorities
            List<GrantedAuthority> grantedAuthorities = List.of();
            if (authoritiesStr != null && !authoritiesStr.isEmpty()) {
                grantedAuthorities = List.of(authoritiesStr.split(","))
                        .stream()
                        .map(String::trim)
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());
            }

            // Create authentication token
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            username,
                            null,
                            grantedAuthorities);

            authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request));

            // Set request attributes for downstream use
            if (organizationId != null) {
                request.setAttribute("organizationId", organizationId);
            }
            request.setAttribute("username", username);

            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception e) {
            SecurityContextHolder.clearContext();
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid user context from Gateway: " + e.getMessage());
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
                // For exists endpoints, only allow GET requests
                if ((pattern.equals("/api/organizations/*/exists") ||
                        pattern.equals("/api/departments/*/exists") ||
                        pattern.equals("/api/teams/*/exists")) && !"GET".equals(method)) {
                    continue;
                }
                // For user endpoints, only allow GET requests
                if ((pattern.equals("/api/departments/user/**") ||
                        pattern.equals("/api/teams/user/**")) && !"GET".equals(method)) {
                    continue;
                }
                return true;
            }
        }

        return false;
    }
} 