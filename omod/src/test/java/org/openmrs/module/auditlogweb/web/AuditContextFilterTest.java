/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.auditlogweb.web;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.module.auditlogweb.api.SecurityAuditContext;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuditContextFilterTest {

    private AuditContextFilter filter;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain filterChain;
    private HttpSession session;
    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        filter = new AuditContextFilter();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        filterChain = mock(FilterChain.class);
        session = mock(HttpSession.class);
    }

    @AfterEach
    void cleanUp() throws Exception {
        SecurityAuditContext.clear();
        if (mocks != null) {
            mocks.close();
        }
    }

    @Test
    void shouldPopulateContextWithoutSession() throws Exception {
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        when(request.getSession(false)).thenReturn(null);

        try (MockedStatic<Context> contextMock = mockStatic(Context.class)) {
            contextMock.when(Context::isAuthenticated).thenReturn(false);

            doAnswer(invocation -> {
                SecurityAuditContext ctx = SecurityAuditContext.get();
                assertNotNull(ctx);
                assertEquals("192.168.1.1", ctx.getIpAddress());
                assertEquals("Mozilla/5.0", ctx.getUserAgent());
                assertNull(ctx.getSessionId());
                assertNull(ctx.getLoggedInUsername());
                return null;
            }).when(filterChain).doFilter(request, response);

            filter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertNull(SecurityAuditContext.get());
        }
    }

    @Test
    void shouldPopulateContextWithSessionAndAuthenticatedUser() throws Exception {
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.195, 70.41.3.18");
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        when(request.getSession(false)).thenReturn(session);
        when(session.getId()).thenReturn("session-test");

        User user = mock(User.class);
        when(user.getUsername()).thenReturn("user1");

        try (MockedStatic<Context> contextMock = mockStatic(Context.class)) {
            contextMock.when(Context::isAuthenticated).thenReturn(true);
            contextMock.when(Context::getAuthenticatedUser).thenReturn(user);

            doAnswer(invocation -> {
                SecurityAuditContext ctx = SecurityAuditContext.get();
                assertNotNull(ctx);
                assertEquals("203.0.113.195", ctx.getIpAddress());
                assertEquals("Mozilla/5.0", ctx.getUserAgent());
                assertEquals("session-test", ctx.getSessionId());
                assertEquals("user1", ctx.getLoggedInUsername());
                return null;
            }).when(filterChain).doFilter(request, response);

            filter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertNull(SecurityAuditContext.get());
        }
    }

    @Test
    void shouldClearContextWhenExceptionThrownInFilterChain() throws Exception {
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("User-Agent")).thenReturn("Safari");
        when(request.getSession(false)).thenReturn(null);

        doThrow(new ServletException("Filter chain exception")).when(filterChain).doFilter(request, response);

        assertThrows(ServletException.class, () -> filter.doFilter(request, response, filterChain));
        assertNull(SecurityAuditContext.get());
    }
}
