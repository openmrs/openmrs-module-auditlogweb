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
import javax.servlet.http.HttpSessionEvent;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.UserContext;
import org.openmrs.module.auditlogweb.api.AuditService;
import org.openmrs.module.auditlogweb.api.listener.ExplicitLogoutSessionTracker;
import org.openmrs.module.auditlogweb.api.listener.LoginFixationSessionTracker;
import org.openmrs.module.auditlogweb.api.utils.AuditSecurityEventType;
import org.openmrs.web.WebConstants;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class SessionTimeoutListenerTest {

    private static final String SESSION_ID = "test-session";

    @Mock
    private AuditService auditService;

    private SessionTimeoutListener listener;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        listener = new SessionTimeoutListener(auditService);
    }

    @AfterEach
    void cleanUp() {
        LoginFixationSessionTracker.consume(SESSION_ID);
        ExplicitLogoutSessionTracker.consume(SESSION_ID);
    }

    @Test
    void shouldAuditActualSessionTimeout(){
        HttpSession newSession = session(SESSION_ID, "admin");

        try (MockedStatic<Context> contextMock = mockStatic(Context.class)) {
            listener.sessionDestroyed(new HttpSessionEvent(newSession));
        }
        verify(auditService).logSecurityEvent(
                AuditSecurityEventType.SESSION_TIMEOUT,
                "admin",
                null,
                null,
                null,
                SESSION_ID,
                null);
    }

    @Test
    void shouldSkipPreLoginSessionDestroyedBySessionFixation() {
        HttpSession oldSession = session(SESSION_ID, null);
        LoginFixationSessionTracker.mark(SESSION_ID);

        listener.sessionDestroyed(new HttpSessionEvent(oldSession));
        verifyNoInteractions(auditService);
    }

    @Test
    void shouldSkipExplicitLogoutSessionTimeout(){
        HttpSession oldSession = session(SESSION_ID, "admin");
        ExplicitLogoutSessionTracker.mark(SESSION_ID);

        listener.sessionDestroyed(new HttpSessionEvent(oldSession));
        verifyNoInteractions(auditService);
    }

    private HttpSession session(String sessionId, String username) {
        HttpSession session = mock(HttpSession.class);
        when(session.getId()).thenReturn(sessionId);
        if (username != null) {
            User user = mock(User.class);
            when(user.getUsername()).thenReturn(username);

            UserContext userContext = mock(UserContext.class);
            when(userContext.getAuthenticatedUser()).thenReturn(user);
            when(session.getAttribute(WebConstants.OPENMRS_USER_CONTEXT_HTTPSESSION_ATTR)).thenReturn(userContext);
        }
        return session;
    }
}
