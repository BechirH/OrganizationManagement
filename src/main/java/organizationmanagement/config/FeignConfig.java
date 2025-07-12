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
                
                // Always set X-Authenticated header for service-to-service calls
                requestTemplate.header("X-Authenticated", "true");
                System.out.println("Organization FeignConfig - Setting X-Authenticated = true");
            } else {
                System.out.println("Organization FeignConfig - No request attributes found");
            }
        };
    }
} 