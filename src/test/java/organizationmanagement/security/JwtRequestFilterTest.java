package organizationmanagement.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import organizationmanagement.config.JwtTokenUtil;

import java.io.IOException;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtRequestFilterTest {

    @Mock
    private JwtTokenUtil jwtTokenUtil;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private JwtRequestFilter jwtRequestFilter;

    @BeforeEach
    void setUp() {
        jwtRequestFilter = new JwtRequestFilter(jwtTokenUtil);
    }

    @Test
    void shouldExtractTokenFromCookie() throws Exception {
        // Given
        String token = "test.jwt.token";
        Cookie[] cookies = {new Cookie("access_token", token)};
        
        lenient().when(request.getRequestURI()).thenReturn("/api/organizations/123");
        lenient().when(request.getMethod()).thenReturn("GET");
        lenient().when(request.getHeader("Authorization")).thenReturn(null);
        lenient().when(request.getCookies()).thenReturn(cookies);
        lenient().when(jwtTokenUtil.isTokenValid(token)).thenReturn(true);
        lenient().when(jwtTokenUtil.extractUsername(token)).thenReturn("testuser");
        lenient().when(jwtTokenUtil.extractOrganizationId(token)).thenReturn(UUID.randomUUID());
        lenient().when(jwtTokenUtil.extractAuthorities(token)).thenReturn(java.util.List.of("ROLE_USER"));

        // When
        jwtRequestFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(response, never()).sendError(anyInt(), anyString());
    }

    @Test
    void shouldExtractTokenFromAuthorizationHeader() throws Exception {
        // Given
        String token = "test.jwt.token";
        String authHeader = "Bearer " + token;
        
        lenient().when(request.getRequestURI()).thenReturn("/api/organizations/123");
        lenient().when(request.getMethod()).thenReturn("GET");
        lenient().when(request.getHeader("Authorization")).thenReturn(authHeader);
        lenient().when(jwtTokenUtil.isTokenValid(token)).thenReturn(true);
        lenient().when(jwtTokenUtil.extractUsername(token)).thenReturn("testuser");
        lenient().when(jwtTokenUtil.extractOrganizationId(token)).thenReturn(UUID.randomUUID());
        lenient().when(jwtTokenUtil.extractAuthorities(token)).thenReturn(java.util.List.of("ROLE_USER"));

        // When
        jwtRequestFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(response, never()).sendError(anyInt(), anyString());
    }

    @Test
    void shouldAllowPublicEndpointsWithoutToken() throws Exception {
        // Given
        lenient().when(request.getRequestURI()).thenReturn("/actuator/health");
        lenient().when(request.getMethod()).thenReturn("GET");

        // When
        jwtRequestFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(response, never()).sendError(anyInt(), anyString());
        verify(jwtTokenUtil, never()).isTokenValid(anyString());
    }

    @Test
    void shouldReturnUnauthorizedWhenNoTokenProvided() throws Exception {
        // Given
        lenient().when(request.getRequestURI()).thenReturn("/api/organizations/123");
        lenient().when(request.getMethod()).thenReturn("GET");
        lenient().when(request.getHeader("Authorization")).thenReturn(null);
        lenient().when(request.getCookies()).thenReturn(null);

        // When
        jwtRequestFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid authentication token");
        verify(filterChain, never()).doFilter(request, response);
    }
} 