/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.auditlogweb.api.listener;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openmrs.User;
import org.openmrs.UserSessionListener.Event;
import org.openmrs.module.auditlogweb.api.AuditService;
import org.openmrs.module.auditlogweb.api.AuditLogContext;
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
        AuditLogContext.clear();
        ExplicitLogoutSessionTracker.consume(SESSION_ID);
    }

    @Test
    void shouldLogLogoutEventWithRequestContext() {
        AuditLogContext context = new AuditLogContext();
        context.setIpAddress("127.0.0.1");
        context.setUserAgent("Mozilla");
        context.setSessionId(SESSION_ID);
        AuditLogContext.set(context);

        when(user.getUsername()).thenReturn("admin");
        when(user.getUuid()).thenReturn("user-uuid-123");

        listener.loggedInOrOut(user, Event.LOGOUT, null);

        verify(auditService).logSecurityEvent(
                AuditSecurityEventType.LOGOUT,
                "admin",
                "user-uuid-123",
                "127.0.0.1",
                "Mozilla",
                SESSION_ID,
                null);
    }

    @Test
    void shouldMarkSessionAsExplicitLogout() {
        AuditLogContext context = new AuditLogContext();
        context.setSessionId(SESSION_ID);
        AuditLogContext.set(context);

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
