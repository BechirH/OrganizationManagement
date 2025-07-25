package organizationmanagement.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collection;
import java.util.UUID;

@Component
public class OrganizationContextUtil {

    /**
     * Extract organization ID from the current request context (set by Gateway)
     */
    public UUID getCurrentOrganizationId() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new SecurityException("No request context found");
        }

        HttpServletRequest request = attributes.getRequest();
        Object organizationIdObj = request.getAttribute("organizationId");
        
        if (organizationIdObj == null) {
            throw new SecurityException("No organization context found in request");
        }

        return (UUID) organizationIdObj;
    }

    /**
     * Check if current user is a root admin (can access all organizations)
     */
    public boolean isRootAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }

        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> authority.equals("SYS_ADMIN_ROOT"));
    }


    /**
     * Validate that the user has access to a specific organization's resource
     */
    public void validateOrganizationAccess(UUID resourceOrganizationId) {
        if (resourceOrganizationId == null) {
            throw new IllegalArgumentException("Resource organization ID cannot be null");
        }

        // Root admins can access all organizations
        if (isRootAdmin()) {
            return;
        }

        UUID currentOrgId = getCurrentOrganizationId();
        if (!currentOrgId.equals(resourceOrganizationId)) {
            throw new SecurityException("Access denied: Resource belongs to different organization");
        }
    }

    /**
     * Get the current user's authorities
     */
    public Collection<? extends GrantedAuthority> getCurrentAuthorities() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? authentication.getAuthorities() : null;
    }



    /**
     * Get current organization ID, returning null if not available (for optional scenarios)
     */
    public UUID getCurrentOrganizationIdOrNull() {
        try {
            return getCurrentOrganizationId();
        } catch (SecurityException e) {
            return null;
        }
    }

    public void validateOrganizationAccess(UUID resourceOrganizationId, String customErrorMessage) {
        if (resourceOrganizationId == null) {
            throw new IllegalArgumentException("Resource organization ID cannot be null");
        }

        // Root admins can access all organizations
        if (isRootAdmin()) {
            return;
        }

        UUID currentOrgId = getCurrentOrganizationId();
        if (!currentOrgId.equals(resourceOrganizationId)) {
            throw new SecurityException(customErrorMessage != null ? customErrorMessage :
                    "Access denied: Resource belongs to different organization");
        }
    }

    /**
     * Check if user has access to specific organization without throwing exception
     */
    public boolean hasOrganizationAccess(UUID organizationId) {
        if (organizationId == null) {
            return false;
        }

        try {
            // Root admins can access all organizations
            if (isRootAdmin()) {
                return true;
            }

            UUID currentOrgId = getCurrentOrganizationId();
            return currentOrgId.equals(organizationId);
        } catch (SecurityException e) {
            return false;
        }
    }



    public boolean hasAuthority(String authority) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }

        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(auth -> auth.equals(authority));
    }
}