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
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import organizationmanagement.config.JwtTokenUtil;

@Component
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
            "/api/organizations/*/exists", // for GET requests
            "/api/departments/*/exists", // for GET requests
            "/api/teams/*/exists", // for GET requests
            "/api/departments/user/**", // for GET requests - THIS WAS MISSING
            "/api/teams/user/**" // for GET requests - THIS WAS MISSING
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
            filterChain.doFilter(request, response);
            return;
        }

        String token = null;
        final String authorizationHeader = request.getHeader("Authorization");
        
        // First try to get token from Authorization header (for backward compatibility)
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            token = authorizationHeader.substring(7);
        } else if (request.getCookies() != null) {
            // Extract token from cookies
            for (Cookie cookie : request.getCookies()) {
                if ("access_token".equals(cookie.getName())) {
                    token = cookie.getValue();
                    break;
                }
            }
        }

        if (token == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid authentication token");
            return;
        }

        try {
            if (!jwtTokenUtil.isTokenValid(token)) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired JWT token");
                return;
            }

            final String username = jwtTokenUtil.extractUsername(token);
            final UUID organizationId = jwtTokenUtil.extractOrganizationId(token);

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
            }
        } catch (Exception e) {
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