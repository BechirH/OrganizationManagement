package organizationmanagement.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GatewayAuthenticationFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private GatewayAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new GatewayAuthenticationFilter();
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldAllowPublicEndpointsWithoutAuthentication() throws Exception {
        // Given
        when(request.getRequestURI()).thenReturn("/actuator/health");
        when(request.getMethod()).thenReturn("GET");

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(response, never()).sendError(anyInt(), anyString());
    }

    @Test
    void shouldAuthenticateWithValidGatewayHeaders() throws Exception {
        // Given
        UUID organizationId = UUID.randomUUID();
        when(request.getRequestURI()).thenReturn("/api/organizations/123");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("X-User-Name")).thenReturn("testuser");
        when(request.getHeader("X-Organization-Id")).thenReturn(organizationId.toString());
        when(request.getHeader("X-User-Authorities")).thenReturn("ROLE_USER,ORGANIZATION_READ");

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(response, never()).sendError(anyInt(), anyString());
        verify(request).setAttribute("organizationId", organizationId);
        verify(request).setAttribute("username", "testuser");
    }

    @Test
    void shouldRejectRequestWithoutUserHeader() throws Exception {
        // Given
        when(request.getRequestURI()).thenReturn("/api/organizations/123");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("X-User-Name")).thenReturn(null);

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing user context from Gateway");
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void shouldHandleInvalidOrganizationId() throws Exception {
        // Given
        when(request.getRequestURI()).thenReturn("/api/organizations/123");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("X-User-Name")).thenReturn("testuser");
        when(request.getHeader("X-Organization-Id")).thenReturn("invalid-uuid");
        when(request.getHeader("X-User-Authorities")).thenReturn("ROLE_USER");

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED, contains("Invalid user context from Gateway"));
        verify(filterChain, never()).doFilter(request, response);
    }
} 