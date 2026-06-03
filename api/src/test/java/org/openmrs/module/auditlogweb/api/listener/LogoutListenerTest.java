package org.openmrs.module.auditlogweb.api.listener;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openmrs.User;
import org.openmrs.UserSessionListener.Event;
import org.openmrs.module.auditlogweb.api.AuditService;
import org.openmrs.module.auditlogweb.api.SecurityAuditContext;
import org.openmrs.module.auditlogweb.api.utils.AuditSecurityEventType;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class LogoutListenerTest {

    private static final String SESSION_ID = "session-123";

    @Mock
    private AuditService auditService;

    @Mock
    private User user;

    private LogoutListener listener;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        listener = new LogoutListener(auditService);
    }

    @AfterEach
    void cleanUp() {
        SecurityAuditContext.clear();
        ExplicitLogoutSessionTracker.consume(SESSION_ID);
    }

    @Test
    void shouldLogLogoutEventWithRequestContext() {
        SecurityAuditContext context = new SecurityAuditContext();
        context.setIpAddress("127.0.0.1");
        context.setUserAgent("Mozilla");
        context.setSessionId(SESSION_ID);
        SecurityAuditContext.set(context);

        when(user.getUsername()).thenReturn("admin");
        when(user.getUserId()).thenReturn(1);

        listener.loggedInOrOut(user, Event.LOGOUT, null);

        verify(auditService).logSecurityEvent(
                AuditSecurityEventType.LOGOUT,
                "admin",
                1,
                "127.0.0.1",
                "Mozilla",
                SESSION_ID,
                null);
    }

    @Test
    void shouldMarkSessionAsExplicitLogout() {
        SecurityAuditContext context = new SecurityAuditContext();
        context.setSessionId(SESSION_ID);
        SecurityAuditContext.set(context);

        listener.loggedInOrOut(user, Event.LOGOUT, null);

        assertTrue(ExplicitLogoutSessionTracker.consume(SESSION_ID));
        assertFalse(ExplicitLogoutSessionTracker.consume(SESSION_ID));
    }

    @Test
    void shouldIgnoreNonLogoutEvents() {
        listener.loggedInOrOut(user, null, null);

        verifyNoInteractions(auditService);
    }
}
