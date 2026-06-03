/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.auditlogweb.api.listener;

import lombok.RequiredArgsConstructor;
import org.openmrs.event.LoginAttemptEvent;
import org.openmrs.module.auditlogweb.api.utils.AuditSecurityEventType;
import org.openmrs.module.auditlogweb.api.AuditService;
import org.openmrs.module.auditlogweb.api.PasswordResetFlowContext;
import org.openmrs.module.auditlogweb.api.SecurityAuditContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

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
                // Marks current pre-fixation session id so SessionTimeoutListener ignores it.
                // The login flow invalidates that session later during fixation protection.
                markSessionAsLoginFixation();
            }
        } catch (Exception e) {
            log.error("Failed to log login attempt for user [{}]", event.getUsername(), e);
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
     * It stamps the current (pre-login) session id with a marker indicating it will be replaced
     * by a new session as part of login session fixation protection.
     * SessionTimeoutListener in omod checks for this marker in {@code sessionDestroyed()}
     * to avoid logging a false SESSION_TIMEOUT when this session is invalidated.
     */
    private void markSessionAsLoginFixation() {
        SecurityAuditContext ctx = SecurityAuditContext.get();
        String sessionId = ctx != null ? ctx.getSessionId() : null;
        if (sessionId == null) return;
        LoginFixationSessionTracker.mark(sessionId);
    }
}
