package org.openmrs.module.auditlogweb.api.listener;

import lombok.RequiredArgsConstructor;
import org.openmrs.User;
import org.openmrs.UserSessionListener;
import org.openmrs.module.auditlogweb.api.AuditService;
import org.openmrs.module.auditlogweb.api.SecurityAuditContext;
import org.openmrs.module.auditlogweb.api.utils.AuditSecurityEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LogoutListener implements UserSessionListener {

    private final Logger log = LoggerFactory.getLogger(LogoutListener.class);
    private final AuditService auditService;

    @Override
    public void loggedInOrOut(User user, Event event, Status status) {
        try{

            if(event != Event.LOGOUT) return;

            SecurityAuditContext ctx = SecurityAuditContext.get();
            String ipAddress = ctx != null ? ctx.getIpAddress() : null;
            String userAgent = ctx != null ? ctx.getUserAgent() : null;
            String sessionId = ctx != null ? ctx.getSessionId() : null;
            String username = user != null ? user.getUsername() : null;

            log.debug("LogoutListener: logging LOGOUT for user [{}]", username);

            if (auditService == null) {
                log.warn("SecurityEventListener: AuditService is not registered, skipping logout audit event");
                return;
            }

            auditService.logSecurityEvent(
                    AuditSecurityEventType.LOGOUT,
                    username,
                    user != null ? user.getUserId() : null,
                    ipAddress,
                    userAgent,
                    sessionId,
                    null);
            log.info("Log out event saved ");
            // Marking the session so SessionTimeoutListener knows this was an explicit logout,
            // not a timeout when the container later calls #sessionDestroyed.
            markSessionAsExplicitLogout(ctx);
        } catch (Exception e) {
            log.error("Failed to log logout event", e);
        }
    }

    /**
     * Marks the session as explicitly logged out. This will be used by the
     * SessionTimeoutListener in omod to determine whether the logout was explicit or not.
     *
     * @param ctx   the security audit context
     */
    private void markSessionAsExplicitLogout(SecurityAuditContext ctx) {
        String sessionId = ctx != null ? ctx.getSessionId() : null;
        if (sessionId == null) {
            return;
        }
        ExplicitLogoutSessionTracker.mark(sessionId);
    }
}
