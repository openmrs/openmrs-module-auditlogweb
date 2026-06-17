/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.auditlogweb.api.aop;


import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.hibernate.SessionFactory;
import org.openmrs.User;
import org.openmrs.api.context.ContextAuthenticationException;
import org.openmrs.module.auditlogweb.api.AuditService;
import org.openmrs.module.auditlogweb.api.PasswordResetFlowContext;
import org.openmrs.module.auditlogweb.api.SecurityAuditContext;
import org.openmrs.module.auditlogweb.api.listener.LoginFixationSessionTracker;
import org.openmrs.module.auditlogweb.api.utils.AuditSecurityEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

// It logs the event related to authentication like login success/failed and account locked.
@Aspect
@Component
@RequiredArgsConstructor
public class AuthenticationAdvice {

    private final static Logger log = LoggerFactory.getLogger(AuthenticationAdvice.class);
    private final AuditService auditService;
    private final SessionFactory sessionFactory;


    @Around("execution(* org.openmrs.api.db.hibernate.HibernateContextDAO.authenticate(..))")
    public Object authenticate(ProceedingJoinPoint joinPoint) throws Throwable {

        SecurityAuditContext ctx = SecurityAuditContext.get();
        String sessionId = ctx != null ? ctx.getSessionId() : null;
        String ipAddress = ctx != null ? ctx.getIpAddress() : null;
        String userAgent = ctx != null ? ctx.getUserAgent() : null;

        try{
            Object result = joinPoint.proceed();
            if(!(result instanceof User)) return result;

            log.debug("Authentication event : LOGIN_SUCCESS");

            // Skipping the audit, because this might be done by system to authenticate user after password reset request verification
            if (PasswordResetFlowContext.hasPendingResetRequest(sessionId)) return result;

            if (auditService == null) {
                log.warn("AuditService is not registered, skipping login audit event");
                return result;
            }

            User user = (User) result;
            safelyLogSecurityEvent(
                    AuditSecurityEventType.LOGIN_SUCCESS,
                    user.getUsername(),
                    user.getUserId(),
                    ipAddress,
                    userAgent,
                    sessionId,
                    "");

            // Marks current pre-fixation session id so SessionTimeoutListener ignores it.
            // The login flow invalidates that session later during fixation protection.
            markSessionAsLoginFixation();

            return result;
        } catch(ContextAuthenticationException ex){

            if (auditService == null) {
                log.warn("AuditService is not registered, skipping login audit event");
                throw ex;
            }

            Object[] args = joinPoint.getArgs();
            String login = null;

            if (args != null && args.length > 0) {
                login = (String) args[0];
            }

            User user = resolveUser(login);
            if(user == null){
                safelyLogSecurityEvent(
                        AuditSecurityEventType.LOGIN_FAILURE,
                        login,
                        null,
                        ipAddress,
                        userAgent,
                        sessionId,
                        buildLoginDetails("INVALID_USERNAME",false));
                throw ex;
            }

            String message = ex.getMessage();
            AuditSecurityEventType eventType = null;
            String reason = null;
            boolean isAccountLocked = false;
            if("Invalid number of connection attempts. Please try again later.".equals(message)){
                log.debug("Authentication event : ACCOUNT_LOCKED");
                eventType = AuditSecurityEventType.ACCOUNT_LOCKED;
                isAccountLocked = true;
                reason = "Too many attempts";
            }else{
                log.debug("Authentication event : LOGIN_FAILURE");
                eventType = AuditSecurityEventType.LOGIN_FAILURE;
                reason = "Invalid credential";
            }
            safelyLogSecurityEvent(
                    eventType,
                    user.getUsername(),
                    user.getUserId(),
                    ipAddress,
                    userAgent,
                    sessionId,
                    buildLoginDetails(reason,isAccountLocked));

            throw ex;
        }
    }

    private void safelyLogSecurityEvent(AuditSecurityEventType eventType, String username, Integer userId,
                                        String ipAddress, String userAgent, String sessionId, String detailsJson) {
        try {
            auditService.logSecurityEvent(eventType, username, userId, ipAddress, userAgent, sessionId, detailsJson);
        } catch (Exception e) {
            log.error("Failed to log authentication security event [{}] for user [{}]", eventType, username, e);
        }
    }

    private User resolveUser(String login){
        if(StringUtils.isBlank(login)) return null;

        User loginUser = null;
        String loginWithDash = login;
        if (login.matches("\\d{2,}")) {
            loginWithDash = login.substring(0, login.length() - 1) + "-" + login.charAt(login.length() - 1);
        }

        try {
            loginUser = sessionFactory.getCurrentSession().createQuery(
                            "from User u where (u.username = ?1 or u.systemId = ?2 or u.systemId = ?3) and u.retired = false",
                            User.class)
                    .setParameter(1, login).setParameter(2, login).setParameter(3, loginWithDash).uniqueResult();
        }
        catch (Exception e) {
            log.error("Error while resolving user for audit");
        }
        return loginUser;
    }

    private String buildLoginDetails(String reason,boolean isAccountLocked) {
        return "{\"failureReason\":\"" + reason + "\",\"accountLocked\":" + isAccountLocked+"}";
    }

    private void markSessionAsLoginFixation() {
        SecurityAuditContext ctx = SecurityAuditContext.get();
        String sessionId = ctx != null ? ctx.getSessionId() : null;
        if (sessionId == null) return;
        LoginFixationSessionTracker.mark(sessionId);
    }
}
