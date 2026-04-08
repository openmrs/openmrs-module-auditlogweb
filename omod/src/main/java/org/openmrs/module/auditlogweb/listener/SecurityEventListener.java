/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.auditlogweb.listener;

import javax.servlet.http.HttpSession;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.event.LoginAttemptEvent;
import org.openmrs.event.LogoutEvent;
import org.openmrs.module.auditlogweb.AuditSecurityEventType;
import org.openmrs.module.auditlogweb.api.AuditService;
import org.openmrs.module.auditlogweb.api.PasswordResetFlowContext;
import org.openmrs.module.auditlogweb.api.SecurityAuditContext;
import org.openmrs.module.auditlogweb.web.AuditContextFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * It receives security-related event like LOGIN, LOGOUT published by
 * openmrs-core and saves them as rows in the audit_security_event table.
 */
@Component
public class SecurityEventListener implements ApplicationListener<ApplicationEvent> {

    private static final Logger log = LoggerFactory.getLogger(SecurityEventListener.class);

    public static final String SESSION_ATTR_EXPLICIT_LOGOUT = "EXPLICIT_LOGOUT";

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        try {

            if (event instanceof LoginAttemptEvent) {
                handleLoginAttempt((LoginAttemptEvent) event);
            } else if (event instanceof LogoutEvent) {
               
                log.info("LOGOUT EVEN CALLED");
               
                handleLogout((LogoutEvent) event);
            }
        } catch (Exception e) {
            log.error("SecurityEventListener: unexpected error while processing event [{}]",
                    event.getClass().getSimpleName(), e);
        }
    }

    private void handleLoginAttempt(LoginAttemptEvent event) {
        AuditSecurityEventType eventType = resolveLoginEventType(event);

        SecurityAuditContext ctx = SecurityAuditContext.get();
        String sessionId = ctx != null ? ctx.getSessionId() : null;
        String ipAddress = ctx != null ? ctx.getIpAddress() : null;
        String userAgent = ctx != null ? ctx.getUserAgent() : null;

        if (event.isSuccess()) {
            // Here skipping the audit , because this might be done by system to authenticate user after password request verification
            if(PasswordResetFlowContext.hasPendingResetRequest(sessionId)){
                return;
            }
        }

        String details = buildLoginDetails(event);

        if (event.isSuccess()) {
            details = "";
            stampLoggedInUserOnSession(event.getUsername());
        }

        log.debug("SecurityEventListener: logging {} for user [{}]", eventType, event.getUsername());

        AuditService auditService = resolveAuditService();
        if (auditService == null) {
            log.warn("SecurityEventListener: AuditService is not registered, skipping login audit event");
            return;
        }

        auditService.logSecurityEvent(
                eventType,
                event.getUsername(),
                event.getUserId(),
                ipAddress,
                userAgent,
                sessionId,
                details);
    }

    private void handleLogout(LogoutEvent event) {
        SecurityAuditContext ctx = SecurityAuditContext.get();
        String ipAddress = ctx != null ? ctx.getIpAddress() : null;
        String userAgent = ctx != null ? ctx.getUserAgent() : null;
        String sessionId = ctx != null ? ctx.getSessionId() : null;
        String username = event.getUsername();

        if (StringUtils.isBlank(username)) {
            HttpSession session = AuditContextFilter.getCurrentSession();
            if (session != null) {
                Object sessionUser = session.getAttribute(AuditContextFilter.SESSION_ATTR_LOGGED_IN_USER);
                if (sessionUser instanceof String) {
                    username = (String) sessionUser;
                }
            }
        }

        log.info("SecurityEventListener: logging LOGOUT for user [{}]", username);

        AuditService auditService = resolveAuditService();
        if (auditService == null) {
            log.warn("SecurityEventListener: AuditService is not registered, skipping logout audit event");
            return;
        }

        auditService.logSecurityEvent(
                AuditSecurityEventType.LOGOUT,
                username,
                event.getUserId(),
                ipAddress,
                userAgent,
                sessionId,
                null);
        log.info("Log out event saved ");
        // Stamp the session so SessionTimeoutListener knows this was an explicit logout
        // (not a timeout) when the container later calls sessionDestroyed().
        markSessionAsExplicitLogout(ctx);
    }

    private AuditSecurityEventType resolveLoginEventType(LoginAttemptEvent event) {
        if (event.isSuccess()) {
            return AuditSecurityEventType.LOGIN_SUCCESS;
        }
        if (event.isAccountLocked()) {
            return AuditSecurityEventType.ACCOUNT_LOCKED;
        }
        return AuditSecurityEventType.LOGIN_FAILURE;
    }

    private String buildLoginDetails(LoginAttemptEvent event) {
        String reason = event.getFailureReason() != null ? event.getFailureReason() : "UNKNOWN";
        return "{\"failureReason\":\"" + reason + "\",\"accountLocked\":" + event.isAccountLocked()+"}";
    }

    private void markSessionAsExplicitLogout(SecurityAuditContext ctx) {
        HttpSession session = AuditContextFilter.getCurrentSession();
        if (session == null) {
            return;
        }
        try {
            session.setAttribute(SESSION_ATTR_EXPLICIT_LOGOUT, Boolean.TRUE);
        } catch (IllegalStateException e) {
            // Session already invalidated - that's fine,we've already recorded the LOGOUT event.
            log.debug("SecurityEventListener: session already invalidated when trying to set EXPLICIT_LOGOUT marker");
        }
    }

    private void stampLoggedInUserOnSession(String username) {
        if (StringUtils.isBlank(username)) {
            return;
        }

        HttpSession session = AuditContextFilter.getCurrentSession();
        if (session == null) {
            return;
        }

        try {
            session.setAttribute(AuditContextFilter.SESSION_ATTR_LOGGED_IN_USER, username);
        } catch (IllegalStateException e) {
            log.debug("SecurityEventListener: session invalidated while trying to set LOGGED_IN_USER");
        }
    }

    /**
     * Lazy lookup of {@link AuditService} from Spring-registered components.
     * This avoids OpenMRS service-registry lookups, which do not include this module service.
     */
    private AuditService resolveAuditService() {
        try {
            return Context.getService(AuditService.class);
        } catch (APIException ex) {
            // Fallback for environments where AuditService is not registered in service registry.
            log.debug("SecurityEventListener: Context.getService(AuditService.class) unavailable, trying registered components");
        }

        List<AuditService> auditServices = Context.getRegisteredComponents(AuditService.class);
        return auditServices.isEmpty() ? null : auditServices.get(0);
    }
}
