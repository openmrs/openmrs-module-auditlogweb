package org.openmrs.module.auditlogweb.api.aop;

import org.aspectj.lang.JoinPoint;
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
import org.openmrs.module.auditlogweb.api.SecurityAuditContext;
import org.openmrs.module.auditlogweb.api.utils.AuditSecurityEventType;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    private JoinPoint joinPoint;

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
        SecurityAuditContext.clear();
        PasswordResetFlowContext.markResetCompleted(SESSION_ID);
        if (mocks != null) {
            mocks.close();
        }
    }

    @Test
    void shouldLogPasswordChangedForManualPasswordChange() throws Exception {
        setRequestContext();
        mockInvocation("changePassword", user, "old-password", "new-password");

        advice.afterReturning(joinPoint, null);

        verify(auditService).logSecurityEvent(
                AuditSecurityEventType.PASSWORD_CHANGED,
                USERNAME,
                1,
                IP_ADDRESS,
                USER_AGENT,
                SESSION_ID,
                "{\"method\":\"changePassword\"}");
    }

    @Test
    void shouldNotIncludeRawPasswordValuesInManualPasswordChangeDetails() throws Exception {
        setRequestContext();
        mockInvocation("changePassword", user, "old-secret", "new-secret");

        advice.afterReturning(joinPoint, null);

        ArgumentCaptor<String> detailsCaptor = ArgumentCaptor.forClass(String.class);
        verify(auditService).logSecurityEvent(
                eq(AuditSecurityEventType.PASSWORD_CHANGED),
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
    void shouldLogSuccessfulPasswordResetRequestAndMarkResetPending() throws Exception {
        setRequestContext();
        mockInvocation("isSecretAnswer", user, "correct-answer");

        advice.afterReturning(joinPoint, Boolean.TRUE);

        verify(auditService).logSecurityEvent(
                AuditSecurityEventType.PASSWORD_RESET_REQUEST,
                USERNAME,
                1,
                IP_ADDRESS,
                USER_AGENT,
                SESSION_ID,
                "{\"requestType\":\"secret_question_answer\", \"isRequestSuccess\": true}");
        assertTrue(PasswordResetFlowContext.hasPendingResetRequest(SESSION_ID));
    }

    @Test
    void shouldLogFailedPasswordResetRequestWithFailureDetails() throws Exception {
        setRequestContext();
        mockInvocation("isSecretAnswer", user, "wrong-answer");

        advice.afterReturning(joinPoint, Boolean.FALSE);

        verify(auditService).logSecurityEvent(
                AuditSecurityEventType.PASSWORD_RESET_REQUEST,
                USERNAME,
                1,
                IP_ADDRESS,
                USER_AGENT,
                SESSION_ID,
                "{\"requestType\":\"secret_question_answer\", \"isRequestSuccess\": false}");
    }

    @Test
    void shouldSkipSystemGeneratedTemporaryPasswordChangeAfterResetRequest() throws Exception {
        setRequestContext();
        PasswordResetFlowContext.markResetRequest(SESSION_ID);
        mockInvocation("changePassword", user, "temporary-password", "new-password");

        advice.afterReturning(joinPoint, null);

        verifyNoInteractions(auditService);
        assertTrue(PasswordResetFlowContext.hasPendingResetRequest(SESSION_ID));
        assertTrue(PasswordResetFlowContext.isPasswordChangedBySystem(SESSION_ID));
    }

    @Test
    void shouldLogPasswordResetAndCompleteFlowAfterSystemPasswordChangeWasSkipped() throws Exception {
        setRequestContext();
        PasswordResetFlowContext.markResetRequest(SESSION_ID);
        PasswordResetFlowContext.setPasswordChangedBySystem(SESSION_ID, true);
        mockInvocation("changePassword", user, "temporary-password", "new-password");

        advice.afterReturning(joinPoint, null);

        verify(auditService).logSecurityEvent(
                AuditSecurityEventType.PASSWORD_RESET,
                USERNAME,
                1,
                IP_ADDRESS,
                USER_AGENT,
                SESSION_ID,
                "{\"method\":\"changePassword\"}");
        assertFalse(PasswordResetFlowContext.hasPendingResetRequest(SESSION_ID));
    }

    @Test
    void shouldLogPasswordChangeWithoutRequestContext() throws Exception {
        mockInvocation("changePassword", user, "old-password", "new-password");

        advice.afterReturning(joinPoint, null);

        verify(auditService).logSecurityEvent(
                AuditSecurityEventType.PASSWORD_CHANGED,
                USERNAME,
                1,
                null,
                null,
                null,
                "{\"method\":\"changePassword\"}");
    }

    @Test
    void shouldSkipLoggingWhenAuditServiceIsNotAvailable() throws Exception {
        PasswordAuditAdvice adviceWithoutAuditService = new PasswordAuditAdvice(null);
        setRequestContext();
        mockInvocation("changePassword", user, "old-password", "new-password");

        adviceWithoutAuditService.afterReturning(joinPoint, null);

        verify(auditService, never()).logSecurityEvent(any(), any(), any(), any(), any(), any(), any());
    }

    private void setRequestContext() {
        SecurityAuditContext context = new SecurityAuditContext();
        context.setIpAddress(IP_ADDRESS);
        context.setUserAgent(USER_AGENT);
        context.setSessionId(SESSION_ID);
        SecurityAuditContext.set(context);
    }

    private void mockInvocation(String methodName, Object... args) throws Exception {
        Method method = UserServiceMethods.class.getMethod(methodName, getParameterTypes(args.length));
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(args);
    }

    private Class<?>[] getParameterTypes(int argumentCount) {
        Class<?>[] parameterTypes = new Class<?>[argumentCount];
        if (argumentCount > 0) {
            parameterTypes[0] = User.class;
        }
        for (int i = 1; i < argumentCount; i++) {
            parameterTypes[i] = String.class;
        }
        return parameterTypes;
    }

    private interface UserServiceMethods {

        boolean isSecretAnswer(User user, String secretAnswer);

        void changePassword(User user, String oldPassword, String newPassword);
    }
}
