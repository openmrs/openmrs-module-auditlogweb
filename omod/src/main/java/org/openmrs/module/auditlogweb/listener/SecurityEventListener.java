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

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.event.LoginAttemptEvent;
import org.openmrs.event.LogoutEvent;
import org.openmrs.module.auditlogweb.AuditlogwebConstants;
import org.openmrs.module.auditlogweb.api.utils.AuditSecurityEventType;
import org.openmrs.module.auditlogweb.api.AuditService;
import org.openmrs.module.auditlogweb.api.PasswordResetFlowContext;
import org.openmrs.module.auditlogweb.api.SecurityAuditContext;
import org.openmrs.module.auditlogweb.web.AuditContextFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import static java.lang.Boolean.TRUE;

/**
 * It receives security-related event from the Spring ApplicationEvent registered on core module for events 
 * like LOGIN, LOGOUT etc. And persists them into audit table.
 */ 
@Component
@RequiredArgsConstructor
public class SecurityEventListener implements ApplicationListener<ApplicationEvent> {

    private static final Logger log = LoggerFactory.getLogger(SecurityEventListener.class);
    private final AuditService auditService;

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        try {

            if (event instanceof LoginAttemptEvent) {
                handleLoginAttempt((LoginAttemptEvent) event);
            } else if (event instanceof LogoutEvent) {
                handleLogout((LogoutEvent) event);
            }
        } catch (Exception e) {
            log.error("SecurityEventListener: unexpected error while processing event [{}]",
                    event.getClass().getSimpleName(), e);
        }
    }

    /**
     * Handles a login attempt like success, failure or account locked.
     * Skips logging if the authentication was triggered internally by the system as part of a password-reset verification flow.
     * 
     * @param event   it's the login attempt event published from the core module
     */
    private void handleLoginAttempt(LoginAttemptEvent event) {

        try {
            AuditSecurityEventType eventType = resolveLoginEventType(event);

            SecurityAuditContext ctx = SecurityAuditContext.get();
            String sessionId = ctx != null ? ctx.getSessionId() : null;
            String ipAddress = ctx != null ? ctx.getIpAddress() : null;
            String userAgent = ctx != null ? ctx.getUserAgent() : null;

            if (event.isSuccess()) {
                // Here skipping the audit, because this might be done by system to authenticate user after password request verification
                if (PasswordResetFlowContext.hasPendingResetRequest(sessionId)) {
                    return;
                }
            }

            String details = buildLoginDetails(event);

            if (event.isSuccess()) {
                details = "";
            }

            log.debug("Logging {} for user [{}]", eventType, event.getUsername());

            if (auditService == null) {
                log.warn("AuditService is not registered, skipping login audit event");
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

            if (event.isSuccess()) {
                // Stamp the current (pre-fixation) session so SessionTimeoutListener can skip it.
                // Spring Security will invalidate this session moments later as part of session
                // fixation protection; without this marker the UserContext already on the session
                // would make SessionTimeoutListener believe it is a genuine timeout.
                markSessionAsLoginFixation();
            }
        } catch (Exception e) {
            log.error("Failed to log login attempt for user [{}]", event.getUsername(), e);
        }
    }

    /**
     * Handles the logout event triggered by the user
     * 
     * @param event   it's the logout event published from the core module
     */
    private void handleLogout(LogoutEvent event) {
        try{
        SecurityAuditContext ctx = SecurityAuditContext.get();
        String ipAddress = ctx != null ? ctx.getIpAddress() : null;
        String userAgent = ctx != null ? ctx.getUserAgent() : null;
        String sessionId = ctx != null ? ctx.getSessionId() : null;
        String username = event.getUsername();

        if (StringUtils.isBlank(username)) {
            HttpSession session = AuditContextFilter.getCurrentSession();
            if (session != null) {
                Object sessionUser = session.getAttribute(AuditlogwebConstants.SESSION_ATTR_LOGGED_IN_USER);
                if (sessionUser instanceof String) {
                    username = (String) sessionUser;
                }
            }
        }

        log.debug("SecurityEventListener: logging LOGOUT for user [{}]", username);

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
        // Stamp the session so SessionTimeoutListener knows this was an explicit logout,
        // not a timeout when the container later calls #sessionDestroyed.
        markSessionAsExplicitLogout(ctx);
        } catch (Exception e) {
            log.error("Failed to log logout event for user [{}]", event.getUsername(), e);
        }
    }

    /**
     * Resolves the login event type based on the login attempt event.
     * 
     * @param event   it's the login attempt event published from the core module
     * @return  the login event type as an enum value
     */
    private AuditSecurityEventType resolveLoginEventType(LoginAttemptEvent event) {
        if (event.isSuccess()) {
            return AuditSecurityEventType.LOGIN_SUCCESS;
        }
        if (event.isAccountLocked()) {
            return AuditSecurityEventType.ACCOUNT_LOCKED;
        }
        return AuditSecurityEventType.LOGIN_FAILURE;
    }

    /**
     * Build a small JSON-like details string which describes the operation for login failure.
     *
     * @param event     any of the login attempt event published by core module
     * @return a details string suitable for storage in the audit record
     */
    private String buildLoginDetails(LoginAttemptEvent event) {
        String reason = event.getFailureReason() != null ? event.getFailureReason() : "UNKNOWN";
        return "{\"failureReason\":\"" + reason + "\",\"accountLocked\":" + event.isAccountLocked()+"}";
    }

    /**
     * Marks the session as explicitly logged out. This will be used by the
     * {@link SessionTimeoutListener} to determine whether the logout was explicit or not.
     * 
     * @param ctx   the security audit context
     */
    private void markSessionAsExplicitLogout(SecurityAuditContext ctx) {
        HttpSession session = AuditContextFilter.getCurrentSession();
        if (session == null) {
            return;
        }
        try {
            session.setAttribute(AuditlogwebConstants.SESSION_ATTR_EXPLICIT_LOGOUT, TRUE);
        } catch (IllegalStateException e) {
            log.warn("Session already invalidated before setting it as EXPLICIT_LOGOUT");
        }
    }

    /**
     * Stamps the current (pre-login) session with a marker indicating it will be replaced
     * by a new session as part of Spring Security's session fixation protection.
     * {@link SessionTimeoutListener} checks for this marker in {@code sessionDestroyed()}
     * to avoid logging a false SESSION_TIMEOUT when this session is invalidated.
     */
    private void markSessionAsLoginFixation() {
        HttpSession session = AuditContextFilter.getCurrentSession();
        if (session == null) {
            return;
        }
        try {
            session.setAttribute(AuditlogwebConstants.SESSION_ATTR_LOGIN_FIXATION, TRUE);
        } catch (IllegalStateException e) {
            log.debug("Session already invalidated before setting LOGIN_FIXATION marker");
        }
    }

}
