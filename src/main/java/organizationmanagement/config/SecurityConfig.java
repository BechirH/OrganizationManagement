package organizationmanagement.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpMethod;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import organizationmanagement.security.JwtRequestFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(
        prePostEnabled = true,
        securedEnabled = true,
        jsr250Enabled = true
)
public class SecurityConfig {

    private final JwtRequestFilter jwtRequestFilter;

    // Public endpoints that don't require authentication
    private static final String[] PUBLIC_ENDPOINTS = {
            "/actuator/health",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    // Endpoints that should be ignored by CSRF protection
    private static final String[] CSRF_IGNORED_ENDPOINTS = {
            "/actuator/health",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/api/organizations", // POST
            "/api/organizations/*/exists", // GET
            "/api/departments/*/exists", // GET
            "/api/teams/*/exists", // GET
            "/api/departments/user/**", // GET
            "/api/teams/user/**" // GET
    };

    public SecurityConfig(JwtRequestFilter jwtRequestFilter) {
        this.jwtRequestFilter = jwtRequestFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // Enable security context propagation for async requests and Feign clients
        SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);

        http
                // Enable CSRF for cookie-based authentication
                .csrf(csrf -> csrf
                    .ignoringRequestMatchers(CSRF_IGNORED_ENDPOINTS) // Allow public and whitelisted endpoints without CSRF
                    .csrfTokenRepository(org.springframework.security.web.csrf.CookieCsrfTokenRepository.withHttpOnlyFalse())
                )
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Configure authorization
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/organizations").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/organizations/*/exists").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/departments/*/exists").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/teams/*/exists").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/departments/user/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/teams/user/**").permitAll()
                        .anyRequest().authenticated()
                )

                // Configure security context propagation
                .securityContext(securityContext -> securityContext
                        .requireExplicitSave(false)  // Required for proper context propagation
                )

                // Stateless session management
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Exception handling
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint())
                        .accessDeniedHandler(jwtAccessDeniedHandler())
                )

                // Add JWT filter
                .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationEntryPoint jwtAuthenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(
                    String.format("{\"error\": \"Unauthorized\", \"message\": \"%s\", \"path\": \"%s\"}",
                            authException.getMessage(), request.getRequestURI())
            );
        };
    }

    @Bean
    public AccessDeniedHandler jwtAccessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write(
                    String.format("{\"error\": \"Forbidden\", \"message\": \"%s\", \"path\": \"%s\"}",
                            accessDeniedException.getMessage(), request.getRequestURI())
            );
        };
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // TODO: Restrict allowed origins for production
        configuration.setAllowedOriginPatterns(java.util.List.of("http://localhost:3000")); // Example: allow only frontend dev server
        configuration.setAllowedMethods(java.util.Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(java.util.Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(java.util.Arrays.asList("Authorization", "Content-Type"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}