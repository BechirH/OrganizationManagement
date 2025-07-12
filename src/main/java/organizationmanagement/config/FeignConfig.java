package organizationmanagement.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Enumeration;

@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            System.out.println("Organization FeignConfig - Intercepting request to: " + requestTemplate.url());
            
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                System.out.println("Organization FeignConfig - Request URI: " + request.getRequestURI());
                
                // Forward authentication headers from Gateway
                Enumeration<String> headerNames = request.getHeaderNames();
                while (headerNames.hasMoreElements()) {
                    String headerName = headerNames.nextElement();
                    String headerValue = request.getHeader(headerName);
                    
                    // Forward user context headers
                    if (headerName.startsWith("X-") || 
                        headerName.equals("Authorization") || 
                        headerName.equals("Cookie")) {
                        requestTemplate.header(headerName, headerValue);
                        System.out.println("Organization FeignConfig - Forwarding header: " + headerName + " = " + headerValue);
                    }
                }
                
                // Explicitly forward key user context headers
                String username = request.getHeader("X-Username");
                String userId = request.getHeader("X-User-Id");
                String organizationId = request.getHeader("X-Organization-Id");
                String authorities = request.getHeader("X-Authorities");
                String userAuthorities = request.getHeader("X-User-Authorities");
                
                if (username != null) {
                    requestTemplate.header("X-Username", username);
                    requestTemplate.header("X-User-Name", username);
                    System.out.println("Organization FeignConfig - Explicitly forwarding X-Username: " + username);
                }
                if (userId != null) {
                    requestTemplate.header("X-User-Id", userId);
                    System.out.println("Organization FeignConfig - Explicitly forwarding X-User-Id: " + userId);
                }
                if (organizationId != null) {
                    requestTemplate.header("X-Organization-Id", organizationId);
                    System.out.println("Organization FeignConfig - Explicitly forwarding X-Organization-Id: " + organizationId);
                }
                if (authorities != null) {
                    requestTemplate.header("X-Authorities", authorities);
                    System.out.println("Organization FeignConfig - Explicitly forwarding X-Authorities: " + authorities);
                }
                if (userAuthorities != null) {
                    requestTemplate.header("X-User-Authorities", userAuthorities);
                    System.out.println("Organization FeignConfig - Explicitly forwarding X-User-Authorities: " + userAuthorities);
                }
                
                // Debug: Print all available headers
                System.out.println("Organization FeignConfig - All available headers:");
                Enumeration<String> allHeaders = request.getHeaderNames();
                while (allHeaders.hasMoreElements()) {
                    String headerName = allHeaders.nextElement();
                    String headerValue = request.getHeader(headerName);
                    System.out.println("  " + headerName + ": " + headerValue);
                }
                
                // Always set X-Authenticated header for service-to-service calls
                requestTemplate.header("X-Authenticated", "true");
                System.out.println("Organization FeignConfig - Setting X-Authenticated = true");
            } else {
                System.out.println("Organization FeignConfig - No request attributes found");
            }
        };
    }
} 