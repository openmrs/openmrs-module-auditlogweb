/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.auditlogweb.api.aop;

import java.lang.reflect.Method;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.module.auditlogweb.api.utils.AuditSecurityEventType;
import org.openmrs.module.auditlogweb.api.AuditService;
import org.openmrs.module.auditlogweb.api.PasswordResetFlowContext;
import org.openmrs.module.auditlogweb.api.SecurityAuditContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.AfterReturningAdvice;
import org.springframework.stereotype.Component;

/**
 * It listens for activity or specifically the password related functions like #isSecretAnswer & #changePassword from the OpenMRS core,
 * and stamps the event like : PASSWORD_CHANGED, PASSWORD_RESET_REQUEST, PASSWORD_RESET
 *
 */
@Component
@RequiredArgsConstructor
public class PasswordAuditAdvice implements AfterReturningAdvice {

    private static final Logger log = LoggerFactory.getLogger(PasswordAuditAdvice.class);
    private final AuditService auditService;

    @Override
    public void afterReturning(Object returnValue, Method method, Object[] args, Object target) {
        String methodName = method.getName();
        boolean isPasswordResetRequestSuccess = false;

        if (!isPasswordMethod(methodName)) return;

        try {
            SecurityAuditContext ctx = SecurityAuditContext.get();
            String sessionId = ctx != null ? ctx.getSessionId() : null;
            String ipAddress = ctx != null ? ctx.getIpAddress() : null;
            String userAgent = ctx != null ? ctx.getUserAgent() : null;

            /* It ignores the #changePassword request triggered automatically by the system after
               #isSecretAnswer successfully verifies the secret answer and it sets a temporary password.
               And then we check if we  already put the reset request on PasswordResetFlowContext during the #isSecretAnswer call which
               confirms that user has requested for password reset request and just few milliseconds later system has reset with temporary pass.
             */
            if("changePassword".equals(methodName) && PasswordResetFlowContext.hasPendingResetRequest(sessionId)
            && !PasswordResetFlowContext.isPasswordChangedBySystem(sessionId)) {
                PasswordResetFlowContext.setPasswordChangedBySystem(sessionId,true);
                return;
            }

            // Reached here means the request is only for verifying the secret answer before allowing the password reset.
            if ("isSecretAnswer".equals(methodName)) {
                PasswordResetFlowContext.markResetRequest(sessionId);
                if(Boolean.TRUE.equals(returnValue)){
                    isPasswordResetRequestSuccess = true;
                }
            }

            /* Reached here means the request is one of the following:
                 1. Secret answer verification succeeded, resulting in PASSWORD_RESET_REQUEST.
                 2. Password reset succeeded after secret answer verification, resulting in PASSWORD_RESET.
                 3. Password has changed manually, resulting in PASSWORD_CHANGED.
             */

            AuditSecurityEventType eventType = resolveEventType(methodName, sessionId);
            String username = getUsername(args);
            Integer userId = getUserId(args);
            String details = buildDetails(methodName,isPasswordResetRequestSuccess);

            if (auditService == null) {
                log.warn("Audit service is not registered, skipping password audit event for method [{}]", methodName);
                return;
            }

            auditService.logSecurityEvent(eventType, username, userId,ipAddress, userAgent, sessionId, details);

            /* Checking whether this #changePassword call was triggered as part of a password reset
              flow after successful secret answer verification. If so, then mark the reset request as completed and remove it.
              This check is performed at the end because we first need to know whether
              #changePassword belongs to a password reset or a normal password change,
              so the correct event can be stored in the database.
             */
            if ("changePassword".equals(methodName) && PasswordResetFlowContext.hasPendingResetRequest(sessionId)
                    && PasswordResetFlowContext.isPasswordChangedBySystem(sessionId)) {
                PasswordResetFlowContext.markResetCompleted(sessionId);
            }

        } catch (Exception e) {
            log.error("Failed to log password audit event for method [{}]", methodName, e);
        }
    }

    /**
     * It checks whether the method name is one we care about for password
     * auditing. Currently, treats `#isSecretAnswer` and `#changePassword` as
     * relevant.
     *
     * @param methodName the method name to check
     * @return true when the method is related to password activity
     */
    private boolean isPasswordMethod(String methodName) {
        return "isSecretAnswer".equals(methodName)
                || "changePassword".equals(methodName);
    }

    /**
     * Resolve the audit event type to log based on the invoked method and
     * the current password reset flow state for the session.
     *
     * @param methodName the invoked method name
     * @param sessionId the session identifier used to check reset state
     * @return the matching AuditSecurityEventType
     */
    private AuditSecurityEventType resolveEventType(String methodName, String sessionId) {

        if ("changePassword".equals(methodName)) {
            if (PasswordResetFlowContext.hasPendingResetRequest(sessionId)) {
                return AuditSecurityEventType.PASSWORD_RESET;
            }
            return AuditSecurityEventType.PASSWORD_CHANGED;
        }

        return AuditSecurityEventType.PASSWORD_RESET_REQUEST;
    }

    private String getUsername(Object[] args) {
        String username = null;

        if (args != null && args.length > 0 && args[0] instanceof User) {
            username = ((User) args[0]).getUsername();
        }

        try {
            if (StringUtils.isBlank(username) && Context.isAuthenticated()) {
                username = Context.getAuthenticatedUser().getUsername();
            }
        } catch (Exception e) {
            log.warn("Could not resolve authenticated username for password audit", e);
        }

        return username;
    }

    private Integer getUserId(Object[] args) {
        Integer userId = null;

        if (args != null && args.length > 0 && args[0] instanceof User) {
            userId = ((User) args[0]).getUserId();
            log.info("Got the userId in resolveUserId: {}", userId);
        }

        try {
            if (userId == null && Context.isAuthenticated()) {
                userId = Context.getAuthenticatedUser().getUserId();
            }
        } catch (Exception e) {
            log.warn("Could not resolve authenticated user ID for password audit", e);
        }

        return userId;
    }

    /**
     * Build a small JSON-like details string which describes the operation.
     *
     * @param methodName the invoked method name
     * @param isPasswordResetRequestSuccess whether the reset request succeeded
     * @return a details string suitable for storage in the audit record
     */
    private String buildDetails(String methodName,boolean isPasswordResetRequestSuccess) {
        if ("setUserActivationKey".equals(methodName)) {
            return "{\"method\":\"setUserActivationKey\",\"requestType\":\"activation_key\"}";
        }
        if ("isSecretAnswer".equals(methodName)) {
            return "{\"requestType\":\"secret_question_answer\", \"isRequestSuccess\": "+isPasswordResetRequestSuccess+"}";
        }
        return "{\"method\":\"" + methodName + "\"}";
    }


}
