package organizationmanagement.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
@Order(1)
public class RequestLoggingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        if (request instanceof HttpServletRequest httpRequest) {
            String requestPath = httpRequest.getRequestURI();
            String method = httpRequest.getMethod();
            
            log.info("Incoming request path: {}", requestPath);
            log.debug("Incoming {} request to: {}", method, requestPath);
        }
        
        chain.doFilter(request, response);
    }
} 