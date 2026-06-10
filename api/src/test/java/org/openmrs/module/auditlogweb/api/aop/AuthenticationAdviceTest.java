/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.auditlogweb.api.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openmrs.User;
import org.openmrs.api.context.ContextAuthenticationException;
import org.openmrs.module.auditlogweb.api.AuditService;
import org.openmrs.module.auditlogweb.api.PasswordResetFlowContext;
import org.openmrs.module.auditlogweb.api.SecurityAuditContext;
import org.openmrs.module.auditlogweb.api.listener.LoginFixationSessionTracker;
import org.openmrs.module.auditlogweb.api.utils.AuditSecurityEventType;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AuthenticationAdviceTest {

    private static final String SESSION_ID = "session-123";
    private static final String IP_ADDRESS = "127.0.0.1";
    private static final String USER_AGENT = "Mozilla";
    private static final String USERNAME = "admin";

    @Mock
    private AuditService auditService;

    @Mock
    private SessionFactory sessionFactory;

    @Mock
    private Session session;

    @Mock
    private Query<User> query;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private User user;

    private AutoCloseable mocks;
    private AuthenticationAdvice advice;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        advice = new AuthenticationAdvice(auditService, sessionFactory);

        when(user.getUsername()).thenReturn(USERNAME);
        when(user.getUserId()).thenReturn(1);
    }

    @AfterEach
    void cleanUp() throws Exception {
        SecurityAuditContext.clear();
        PasswordResetFlowContext.markResetCompleted(SESSION_ID);
        LoginFixationSessionTracker.consume(SESSION_ID);
        if (mocks != null) {
            mocks.close();
        }
    }

    @Test
    void shouldLogSuccessfulLoginAndMarkSessionAsLoginFixation() throws Throwable {
        setRequestContext();
        when(joinPoint.proceed()).thenReturn(user);

        Object result = advice.authenticate(joinPoint);

        assertSame(user, result);
        verify(auditService).logSecurityEvent(
                AuditSecurityEventType.LOGIN_SUCCESS,
                USERNAME,
                1,
                IP_ADDRESS,
                USER_AGENT,
                SESSION_ID,
                "");
        assertTrue(LoginFixationSessionTracker.consume(SESSION_ID));
        assertFalse(LoginFixationSessionTracker.consume(SESSION_ID));
    }

    @Test
    void shouldSkipSuccessfulLoginAuditDuringPendingPasswordReset() throws Throwable {
        setRequestContext();
        PasswordResetFlowContext.markResetRequest(SESSION_ID);
        when(joinPoint.proceed()).thenReturn(user);

        Object result = advice.authenticate(joinPoint);

        assertSame(user, result);
        verifyNoInteractions(auditService, sessionFactory);
        assertFalse(LoginFixationSessionTracker.consume(SESSION_ID));
    }

    @Test
    void shouldSkipSuccessfulLoginAuditWhenAuditServiceIsUnavailable() throws Throwable {
        AuthenticationAdvice adviceWithoutAuditService = new AuthenticationAdvice(null, sessionFactory);
        setRequestContext();
        when(joinPoint.proceed()).thenReturn(user);

        Object result = adviceWithoutAuditService.authenticate(joinPoint);

        assertSame(user, result);
        verifyNoInteractions(sessionFactory);
        assertFalse(LoginFixationSessionTracker.consume(SESSION_ID));
    }

    @Test
    void shouldLogInvalidUsernameFailureAndRethrowAuthenticationException() throws Throwable {
        setRequestContext();
        ContextAuthenticationException exception = authenticationFailure("unknown", "Login failed");
        mockUserLookup("unknown", "unknown", null);

        ContextAuthenticationException thrown = assertThrows(
                ContextAuthenticationException.class,
                () -> advice.authenticate(joinPoint));

        assertSame(exception, thrown);
        verify(auditService).logSecurityEvent(
                AuditSecurityEventType.LOGIN_FAILURE,
                "unknown",
                null,
                IP_ADDRESS,
                USER_AGENT,
                SESSION_ID,
                "{\"failureReason\":\"INVALID_USERNAME\",\"accountLocked\":false}");
    }

    @Test
    void shouldLogInvalidCredentialFailureForResolvedUser() throws Throwable {
        setRequestContext();
        ContextAuthenticationException exception = authenticationFailure(USERNAME, "Login failed");
        mockUserLookup(USERNAME, USERNAME, user);

        ContextAuthenticationException thrown = assertThrows(
                ContextAuthenticationException.class,
                () -> advice.authenticate(joinPoint));

        assertSame(exception, thrown);
        verify(auditService).logSecurityEvent(
                AuditSecurityEventType.LOGIN_FAILURE,
                USERNAME,
                1,
                IP_ADDRESS,
                USER_AGENT,
                SESSION_ID,
                "{\"failureReason\":\"Invalid credential\",\"accountLocked\":false}");
    }

    @Test
    void shouldLogAccountLockedEventForTooManyAttempts() throws Throwable {
        setRequestContext();
        ContextAuthenticationException exception = authenticationFailure(
                USERNAME,
                "Invalid number of connection attempts. Please try again later.");
        mockUserLookup(USERNAME, USERNAME, user);

        ContextAuthenticationException thrown = assertThrows(
                ContextAuthenticationException.class,
                () -> advice.authenticate(joinPoint));

        assertSame(exception, thrown);
        verify(auditService).logSecurityEvent(
                AuditSecurityEventType.ACCOUNT_LOCKED,
                USERNAME,
                1,
                IP_ADDRESS,
                USER_AGENT,
                SESSION_ID,
                "{\"failureReason\":\"Too many attempts\",\"accountLocked\":true}");
    }

    @Test
    void shouldTreatBlankLoginAsInvalidUsernameWithoutQueryingDatabase() throws Throwable {
        authenticationFailure(" ", "Login failed");

        assertThrows(ContextAuthenticationException.class, () -> advice.authenticate(joinPoint));

        verifyNoInteractions(sessionFactory);
        verify(auditService).logSecurityEvent(
                AuditSecurityEventType.LOGIN_FAILURE,
                " ",
                null,
                null,
                null,
                null,
                "{\"failureReason\":\"INVALID_USERNAME\",\"accountLocked\":false}");
    }

    @Test
    void shouldLogInvalidUsernameWhenUserLookupFails() throws Throwable {
        setRequestContext();
        authenticationFailure(USERNAME, "Login failed");
        when(sessionFactory.getCurrentSession()).thenThrow(new IllegalStateException("No session"));

        assertThrows(ContextAuthenticationException.class, () -> advice.authenticate(joinPoint));

        verify(auditService).logSecurityEvent(
                AuditSecurityEventType.LOGIN_FAILURE,
                USERNAME,
                null,
                IP_ADDRESS,
                USER_AGENT,
                SESSION_ID,
                "{\"failureReason\":\"INVALID_USERNAME\",\"accountLocked\":false}");
    }

    private ContextAuthenticationException authenticationFailure(String login, String message) throws Throwable {
        ContextAuthenticationException exception = new ContextAuthenticationException(message);
        when(joinPoint.proceed()).thenThrow(exception);
        when(joinPoint.getArgs()).thenReturn(new Object[] { login });
        return exception;
    }

    private void mockUserLookup(String login, String dashedLogin, User result) {
        when(sessionFactory.getCurrentSession()).thenReturn(session);
        when(session.createQuery(any(String.class), any(Class.class))).thenReturn(query);
        when(query.setParameter(1, login)).thenReturn(query);
        when(query.setParameter(2, login)).thenReturn(query);
        when(query.setParameter(3, dashedLogin)).thenReturn(query);
        when(query.uniqueResult()).thenReturn(result);
    }

    private void setRequestContext() {
        SecurityAuditContext context = new SecurityAuditContext();
        context.setIpAddress(IP_ADDRESS);
        context.setUserAgent(USER_AGENT);
        context.setSessionId(SESSION_ID);
        SecurityAuditContext.set(context);
    }
}
