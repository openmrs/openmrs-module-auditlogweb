/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.auditlogweb.advice;

import java.lang.reflect.Method;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.module.auditlogweb.AuditSecurityEventType;
import org.openmrs.module.auditlogweb.api.AuditService;
import org.openmrs.module.auditlogweb.api.PasswordResetFlowContext;
import org.openmrs.module.auditlogweb.api.SecurityAuditContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.AfterReturningAdvice;

/**
 * Listen for activity related to password audit 
 */
public class PasswordAuditAdvice implements AfterReturningAdvice {

    private static final Logger log = LoggerFactory.getLogger(PasswordAuditAdvice.class);

    @Override
    public void afterReturning(Object returnValue, Method method, Object[] args, Object target) {
        String methodName = method.getName();
        boolean isPasswordResetRequestSuccess = true;

        if (!isPasswordMethod(methodName)) {
            return;
        }

        if ("isSecretAnswer".equals(methodName) && !Boolean.TRUE.equals(returnValue)) {
            isPasswordResetRequestSuccess = false;
        }

        try {
            SecurityAuditContext ctx = SecurityAuditContext.get();
            String sessionId = ctx != null ? ctx.getSessionId() : null;
            String ipAddress = ctx != null ? ctx.getIpAddress() : null;
            String userAgent = ctx != null ? ctx.getUserAgent() : null;

            // Here it ignores the changePassword request which triggered by system after just it verifies
            // the secret question answer via isSecretAnswer,
            if("changePassword".equals(methodName) && PasswordResetFlowContext.hasPendingResetRequest(sessionId)
            && !PasswordResetFlowContext.isPasswordChangedBySystem(sessionId)) {
                PasswordResetFlowContext.setPasswordChangedBySystem(sessionId,true);
                return;
            }

            if ("isSecretAnswer".equals(methodName)) {
                PasswordResetFlowContext.markResetRequest(sessionId, getUsername(args), getUserId(args));
            }

            AuditSecurityEventType eventType = resolveEventType(methodName, sessionId);
            String username = getUsername(args);
            Integer userId = getUserId(args);
            String details = buildDetails(methodName,isPasswordResetRequestSuccess);

            AuditService auditService = resolveAuditService();
            if (auditService == null) {
                log.warn("Audit service is not registered, skipping password audit event for method [{}]", methodName);
                return;
            }

            auditService.logSecurityEvent(eventType, username, userId,
                    ipAddress, userAgent, sessionId, details);

            if ("changePassword".equals(methodName) && PasswordResetFlowContext.hasPendingResetRequest(sessionId)
                    && PasswordResetFlowContext.isPasswordChangedBySystem(sessionId)) {
                PasswordResetFlowContext.markResetCompleted(sessionId);
            }

        } catch (Exception e) {
            log.error("Failed to log password audit event for method [{}]", methodName, e);
        }
    }

    private boolean isPasswordMethod(String methodName) {
        // Listening to "isSecretAnswer" so that we can track the password change request, because this is point
        // or call, which comes to verify the secrets before making request to change to password.
        return "isSecretAnswer".equals(methodName)
                || "changePassword".equals(methodName);
    }


    private boolean isPasswordResetRequestMethod(String methodName) {
        return "isSecretAnswer".equals(methodName);
    }

    private AuditSecurityEventType resolveEventType(String methodName, String sessionId) {
        if ("changePassword".equals(methodName)) {
            if (PasswordResetFlowContext.hasPendingResetRequest(sessionId)) {
                return AuditSecurityEventType.PASSWORD_RESET;
            }
            return AuditSecurityEventType.PASSWORD_CHANGED;
        }
        if (isPasswordResetRequestMethod(methodName)) {
            return AuditSecurityEventType.PASSWORD_RESET_REQUEST;
        }
        return AuditSecurityEventType.PASSWORD_RESET;
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

    private String buildDetails(String methodName,boolean isPasswordResetRequestSuccess) {
        if ("setUserActivationKey".equals(methodName)) {
            return "{\"method\":\"setUserActivationKey\",\"requestType\":\"activation_key\"}";
        }
        if ("isSecretAnswer".equals(methodName)) {
            return "{\"requestType\":\"secret_question_answer\", \"isRequestSuccess\": "+isPasswordResetRequestSuccess+"}";
        }
        return "{\"method\":\"" + methodName + "\"}";
    }

    private AuditService resolveAuditService() {
        List<AuditService> auditServices = Context.getRegisteredComponents(AuditService.class);
        return auditServices.isEmpty() ? null : auditServices.get(0);
    }
}
