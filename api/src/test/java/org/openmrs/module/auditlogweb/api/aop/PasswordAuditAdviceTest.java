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
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openmrs.User;
import org.openmrs.module.auditlogweb.api.AuditService;
import org.openmrs.module.auditlogweb.api.PasswordResetFlowContext;
import org.openmrs.module.auditlogweb.api.AuditLogContext;
import org.openmrs.module.auditlogweb.api.utils.AuditSecurityEventType;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PasswordAuditAdviceTest {

    private static final String SESSION_ID = "session-123";
    private static final String IP_ADDRESS = "127.0.0.1";
    private static final String USER_AGENT = "Mozilla";
    private static final String USERNAME = "admin";

    @Mock
    private AuditService auditService;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    @Mock
    private User user;

    private AutoCloseable mocks;
    private PasswordAuditAdvice advice;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        advice = new PasswordAuditAdvice(auditService);

        when(joinPoint.getTarget()).thenReturn(new Object());
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(user.getUsername()).thenReturn(USERNAME);
        when(user.getUserId()).thenReturn(1);
    }

    @AfterEach
    void cleanUp() throws Exception {
        AuditLogContext.clear();
        PasswordResetFlowContext.markResetCompleted(SESSION_ID);
        if (mocks != null) {
            mocks.close();
        }
    }

    @Test
    void shouldLogPasswordChangedForManualPasswordChange() throws Throwable {
        setRequestContext();
        mockInvocation("changePassword", user, "old-password", "new-password");
        when(joinPoint.proceed()).thenReturn(null);

        advice.auditPasswordActivity(joinPoint);

        verify(auditService).logSecurityEvent(
                AuditSecurityEventType.PASSWORD_CHANGED_SUCCESS,
                USERNAME,
                1,
                IP_ADDRESS,
                USER_AGENT,
                SESSION_ID,
                "{\"method\":\"changePassword\"}");
    }

    @Test
    void shouldNotIncludeRawPasswordValuesInManualPasswordChangeDetails() throws Throwable {
        setRequestContext();
        mockInvocation("changePassword", user, "old-secret", "new-secret");
        when(joinPoint.proceed()).thenReturn(null);

        advice.auditPasswordActivity(joinPoint);

        ArgumentCaptor<String> detailsCaptor = ArgumentCaptor.forClass(String.class);
        verify(auditService).logSecurityEvent(
                eq(AuditSecurityEventType.PASSWORD_CHANGED_SUCCESS),
                eq(USERNAME),
                eq(1),
                any(),
                any(),
                eq(SESSION_ID),
                detailsCaptor.capture());

        assertFalse(detailsCaptor.getValue().contains("old-secret"));
        assertFalse(detailsCaptor.getValue().contains("new-secret"));
    }

    @Test
    void shouldLogSuccessfulPasswordResetRequestAndMarkResetPending() throws Throwable {
        setRequestContext();
        mockInvocation("isSecretAnswer", user, "correct-answer");
        when(joinPoint.proceed()).thenReturn(Boolean.TRUE);

        advice.auditPasswordActivity(joinPoint);

        verify(auditService).logSecurityEvent(
                AuditSecurityEventType.PASSWORD_RESET_REQUEST_SUCCESS,
                USERNAME,
                1,
                IP_ADDRESS,
                USER_AGENT,
                SESSION_ID,
                "{\"requestType\":\"secret_question_answer\", \"isRequestSuccess\": true}");
        assertTrue(PasswordResetFlowContext.hasPendingResetRequest(SESSION_ID));
        assertTrue(PasswordResetFlowContext.isSecretAnswerVerified(SESSION_ID));
    }

    @Test
    void shouldLogFailedPasswordResetRequestWithFailureDetails() throws Throwable {
        setRequestContext();
        mockInvocation("isSecretAnswer", user, "wrong-answer");
        when(joinPoint.proceed()).thenReturn(Boolean.FALSE);

        advice.auditPasswordActivity(joinPoint);

        verify(auditService).logSecurityEvent(
                AuditSecurityEventType.PASSWORD_RESET_REQUEST_FAILURE,
                USERNAME,
                1,
                IP_ADDRESS,
                USER_AGENT,
                SESSION_ID,
                "{\"requestType\":\"secret_question_answer\", \"isRequestSuccess\": false}");
        assertFalse(PasswordResetFlowContext.isSecretAnswerVerified(SESSION_ID));
    }

    @Test
    void shouldSkipSystemGeneratedTemporaryPasswordChangeAfterResetRequest() throws Throwable {
        setRequestContext();
        PasswordResetFlowContext.markResetRequest(SESSION_ID);
        mockInvocation("changePassword", user, "temporary-password", "new-password");
        when(joinPoint.proceed()).thenReturn(null);

        advice.auditPasswordActivity(joinPoint);

        verifyNoInteractions(auditService);
        assertTrue(PasswordResetFlowContext.hasPendingResetRequest(SESSION_ID));
        assertTrue(PasswordResetFlowContext.isPasswordChangedBySystem(SESSION_ID));
    }

    @Test
    void shouldLogPasswordResetAndCompleteFlowAfterSystemPasswordChangeWasSkipped() throws Throwable {
        setRequestContext();
        PasswordResetFlowContext.markResetRequest(SESSION_ID);
        PasswordResetFlowContext.setPasswordChangedBySystem(SESSION_ID, true);
        mockInvocation("changePassword", user, "temporary-password", "new-password");
        when(joinPoint.proceed()).thenReturn(null);

        advice.auditPasswordActivity(joinPoint);

        verify(auditService).logSecurityEvent(
                AuditSecurityEventType.PASSWORD_RESET_SUCCESS,
                USERNAME,
                1,
                IP_ADDRESS,
                USER_AGENT,
                SESSION_ID,
                "{\"method\":\"changePassword\"}");
        assertFalse(PasswordResetFlowContext.hasPendingResetRequest(SESSION_ID));
    }

    @Test
    void shouldLogPasswordChangeWithoutRequestContext() throws Throwable {
        mockInvocation("changePassword", user, "old-password", "new-password");
        when(joinPoint.proceed()).thenReturn(null);

        advice.auditPasswordActivity(joinPoint);

        verify(auditService).logSecurityEvent(
                AuditSecurityEventType.PASSWORD_CHANGED_SUCCESS,
                USERNAME,
                1,
                null,
                null,
                null,
                "{\"method\":\"changePassword\"}");
    }

    @Test
    void shouldSkipLoggingWhenAuditServiceIsNotAvailable() throws Throwable {
        PasswordAuditAdvice adviceWithoutAuditService = new PasswordAuditAdvice(null);
        setRequestContext();
        mockInvocation("changePassword", user, "old-password", "new-password");
        when(joinPoint.proceed()).thenReturn(null);

        adviceWithoutAuditService.auditPasswordActivity(joinPoint);

        verify(auditService, never()).logSecurityEvent(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void shouldCompleteResetFlowEvenIfAuditServiceThrowsException() throws Throwable {
        setRequestContext();
        PasswordResetFlowContext.markResetRequest(SESSION_ID);
        PasswordResetFlowContext.setPasswordChangedBySystem(SESSION_ID, true);
        mockInvocation("changePassword", user, "temporary-password", "new-password");
        when(joinPoint.proceed()).thenReturn(null);

        org.mockito.Mockito.doThrow(new RuntimeException("Database error"))
                .when(auditService).logSecurityEvent(any(), any(), any(), any(), any(), any(), any());

        advice.auditPasswordActivity(joinPoint);

        assertFalse(PasswordResetFlowContext.hasPendingResetRequest(SESSION_ID));
    }

    @Test
    void shouldLogPasswordChangedFailureForManualPasswordChangeWhenExceptionThrown() throws Throwable {
        setRequestContext();
        mockInvocation("changePassword", user, "old-password", "new-password");
        RuntimeException testEx = new RuntimeException("Validation error");
        when(joinPoint.proceed()).thenThrow(testEx);

        Exception thrown = assertThrows(
                RuntimeException.class,
                () -> advice.auditPasswordActivity(joinPoint));

        assertSame(testEx, thrown);
        verify(auditService).logSecurityEvent(
                AuditSecurityEventType.PASSWORD_CHANGED_FAILURE,
                USERNAME,
                1,
                IP_ADDRESS,
                USER_AGENT,
                SESSION_ID,
                "{\"method\":\"changePassword\"}");
    }

    @Test
    void shouldLogPasswordResetFailureAfterResetRequestWhenExceptionThrown() throws Throwable {
        setRequestContext();
        PasswordResetFlowContext.markResetRequest(SESSION_ID);
        PasswordResetFlowContext.setPasswordChangedBySystem(SESSION_ID, true);
        mockInvocation("changePassword", user, "temporary-password", "new-password");
        RuntimeException testEx = new RuntimeException("Verification error");
        when(joinPoint.proceed()).thenThrow(testEx);

        Exception thrown = assertThrows(
                RuntimeException.class,
                () -> advice.auditPasswordActivity(joinPoint));

        assertSame(testEx, thrown);
        verify(auditService).logSecurityEvent(
                AuditSecurityEventType.PASSWORD_RESET_FAILURE,
                USERNAME,
                1,
                IP_ADDRESS,
                USER_AGENT,
                SESSION_ID,
                "{\"method\":\"changePassword\"}");
    }

    @Test
    void shouldLogPasswordResetRequestFailureWhenExceptionThrown() throws Throwable {
        setRequestContext();
        mockInvocation("isSecretAnswer", user, "some-answer");
        RuntimeException testEx = new RuntimeException("DB offline");
        when(joinPoint.proceed()).thenThrow(testEx);

        Exception thrown = assertThrows(
                RuntimeException.class,
                () -> advice.auditPasswordActivity(joinPoint));

        assertSame(testEx, thrown);
        verify(auditService).logSecurityEvent(
                AuditSecurityEventType.PASSWORD_RESET_REQUEST_FAILURE,
                USERNAME,
                1,
                IP_ADDRESS,
                USER_AGENT,
                SESSION_ID,
                "{\"requestType\":\"secret_question_answer\", \"isRequestSuccess\": false}");
    }

    @Test
    void shouldLogPasswordResetSuccessForChangePasswordUsingSecretAnswer() throws Throwable {
        setRequestContext();
        mockInvocation("changePasswordUsingSecretAnswer", "correct-answer", "new-password");
        when(joinPoint.proceed()).thenReturn(null);

        advice.auditPasswordActivity(joinPoint);

        verify(auditService).logSecurityEvent(
                AuditSecurityEventType.PASSWORD_RESET_SUCCESS,
                null,
                null,
                IP_ADDRESS,
                USER_AGENT,
                SESSION_ID,
                "{\"method\":\"changePasswordUsingSecretAnswer\"}");
    }

    @Test
    void shouldLogPasswordResetFailureForChangePasswordUsingSecretAnswerWhenExceptionThrown() throws Throwable {
        setRequestContext();
        mockInvocation("changePasswordUsingSecretAnswer", "correct-answer", "new-password");
        RuntimeException testEx = new RuntimeException("Incorrect secret answer");
        when(joinPoint.proceed()).thenThrow(testEx);

        Exception thrown = assertThrows(
                RuntimeException.class,
                () -> advice.auditPasswordActivity(joinPoint));

        assertSame(testEx, thrown);
        verify(auditService).logSecurityEvent(
                AuditSecurityEventType.PASSWORD_RESET_FAILURE,
                null,
                null,
                IP_ADDRESS,
                USER_AGENT,
                SESSION_ID,
                "{\"method\":\"changePasswordUsingSecretAnswer\"}");
    }

    @Test
    void shouldLogPasswordResetSuccessForChangePasswordUsingActivationKey() throws Throwable {
        setRequestContext();
        mockInvocation("changePasswordUsingActivationKey", "activation-key", "new-password");
        when(joinPoint.proceed()).thenReturn(null);

        advice.auditPasswordActivity(joinPoint);

        verify(auditService).logSecurityEvent(
                AuditSecurityEventType.PASSWORD_RESET_SUCCESS,
                null,
                null,
                IP_ADDRESS,
                USER_AGENT,
                SESSION_ID,
                "{\"method\":\"changePasswordUsingActivationKey\"}");
    }

    @Test
    void shouldLogPasswordResetFailureForChangePasswordUsingActivationKeyWhenExceptionThrown() throws Throwable {
        setRequestContext();
        mockInvocation("changePasswordUsingActivationKey", "activation-key", "new-password");
        RuntimeException testEx = new RuntimeException("Invalid key");
        when(joinPoint.proceed()).thenThrow(testEx);

        Exception thrown = assertThrows(
                RuntimeException.class,
                () -> advice.auditPasswordActivity(joinPoint));

        assertSame(testEx, thrown);
        verify(auditService).logSecurityEvent(
                AuditSecurityEventType.PASSWORD_RESET_FAILURE,
                null,
                null,
                IP_ADDRESS,
                USER_AGENT,
                SESSION_ID,
                "{\"method\":\"changePasswordUsingActivationKey\"}");
    }

    private void setRequestContext() {
        AuditLogContext context = new AuditLogContext();
        context.setIpAddress(IP_ADDRESS);
        context.setUserAgent(USER_AGENT);
        context.setSessionId(SESSION_ID);
        AuditLogContext.set(context);
    }

    private void mockInvocation(String methodName, Object... args) throws Exception {
        Method method = UserServiceMethods.class.getMethod(methodName, getParameterTypes(args));
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(args);
    }

    private Class<?>[] getParameterTypes(Object[] args) {
        Class<?>[] parameterTypes = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof User) {
                parameterTypes[i] = User.class;
            } else {
                parameterTypes[i] = String.class;
            }
        }
        return parameterTypes;
    }

    private interface UserServiceMethods {

        boolean isSecretAnswer(User user, String secretAnswer);

        void changePassword(User user, String oldPassword, String newPassword);

        void changePasswordUsingSecretAnswer(String secretAnswer, String pw);

        void changePasswordUsingActivationKey(String activationKey, String newPassword);
    }
}
