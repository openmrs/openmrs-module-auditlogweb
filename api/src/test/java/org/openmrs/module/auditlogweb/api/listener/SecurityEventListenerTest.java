package org.openmrs.module.auditlogweb.api.listener;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openmrs.event.LoginAttemptEvent;
import org.openmrs.module.auditlogweb.api.AuditService;
import org.openmrs.module.auditlogweb.api.PasswordResetFlowContext;
import org.openmrs.module.auditlogweb.api.SecurityAuditContext;
import org.openmrs.module.auditlogweb.api.utils.AuditSecurityEventType;
import org.springframework.context.ApplicationEvent;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SecurityEventListenerTest {

    private static final String SESSION_ID = "session-123";

    @Mock
    private AuditService auditService;

    @Mock
    private LoginAttemptEvent loginAttemptEvent;

    private SecurityEventListener listener;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        listener = new SecurityEventListener(auditService);
    }

    @AfterEach
    void cleanUp() {
        SecurityAuditContext.clear();
        LoginFixationSessionTracker.consume(SESSION_ID);
        PasswordResetFlowContext.markResetCompleted(SESSION_ID);
    }

    @Test
    void shouldLogSuccessfulLoginAndMarkSessionForFixationSkip() {
        SecurityAuditContext.set(securityAuditContext());
        when(loginAttemptEvent.isSuccess()).thenReturn(true);
        when(loginAttemptEvent.getUsername()).thenReturn("admin");
        when(loginAttemptEvent.getUserId()).thenReturn(1);

        listener.onApplicationEvent(loginAttemptEvent);

        verify(auditService).logSecurityEvent(
                AuditSecurityEventType.LOGIN_SUCCESS,
                "admin",
                1,
                "127.0.0.1",
                "Mozilla",
                SESSION_ID,
                "");
        assertTrue(LoginFixationSessionTracker.consume(SESSION_ID));
        assertFalse(LoginFixationSessionTracker.consume(SESSION_ID));
    }

    @Test
    void shouldLogFailedLoginWithFailureDetails() {
        SecurityAuditContext.set(securityAuditContext());
        when(loginAttemptEvent.isSuccess()).thenReturn(false);
        when(loginAttemptEvent.isAccountLocked()).thenReturn(false);
        when(loginAttemptEvent.getUsername()).thenReturn("admin");
        when(loginAttemptEvent.getUserId()).thenReturn(1);
        when(loginAttemptEvent.getFailureReason()).thenReturn("INVALID_CREDENTIALS");

        listener.onApplicationEvent(loginAttemptEvent);

        verify(auditService).logSecurityEvent(
                AuditSecurityEventType.LOGIN_FAILURE,
                "admin",
                1,
                "127.0.0.1",
                "Mozilla",
                SESSION_ID,
                "{\"failureReason\":\"INVALID_CREDENTIALS\",\"accountLocked\":false}");
    }

    @Test
    void shouldLogAccountLockedLoginAttempt() {
        when(loginAttemptEvent.isSuccess()).thenReturn(false);
        when(loginAttemptEvent.isAccountLocked()).thenReturn(true);
        when(loginAttemptEvent.getUsername()).thenReturn("admin");
        when(loginAttemptEvent.getUserId()).thenReturn(1);
        when(loginAttemptEvent.getFailureReason()).thenReturn("ACCOUNT_LOCKED");

        listener.onApplicationEvent(loginAttemptEvent);

        verify(auditService).logSecurityEvent(
                AuditSecurityEventType.ACCOUNT_LOCKED,
                "admin",
                1,
                null,
                null,
                null,
                "{\"failureReason\":\"ACCOUNT_LOCKED\",\"accountLocked\":true}");
    }

    @Test
    void shouldIgnoreNonLoginAttemptEvents() {
        ApplicationEvent event = new ApplicationEvent("source") {};

        listener.onApplicationEvent(event);

        verifyNoInteractions(auditService);
    }

    @Test
    void shouldSkipSuccessfulLoginDuringPendingPasswordResetFlow() {
        SecurityAuditContext.set(securityAuditContext());
        PasswordResetFlowContext.markResetRequest(SESSION_ID);
        when(loginAttemptEvent.isSuccess()).thenReturn(true);

        listener.onApplicationEvent(loginAttemptEvent);

        verifyNoInteractions(auditService);
        assertFalse(LoginFixationSessionTracker.consume(SESSION_ID));
    }

    private SecurityAuditContext securityAuditContext() {
        SecurityAuditContext context = new SecurityAuditContext();
        context.setIpAddress("127.0.0.1");
        context.setUserAgent("Mozilla");
        context.setSessionId(SESSION_ID);
        return context;
    }
}
