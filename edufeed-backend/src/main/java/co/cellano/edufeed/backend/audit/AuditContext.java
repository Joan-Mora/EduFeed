package co.cellano.edufeed.backend.audit;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

public final class AuditContext {
    private static final String SISTEMA = "SISTEMA";

    private AuditContext() {
    }

    public static String getCurrentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return SISTEMA;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof String s && "anonymousUser".equals(s)) {
            return SISTEMA;
        }
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        return authentication.getName() != null ? authentication.getName() : SISTEMA;
    }
}
