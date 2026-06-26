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
import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.module.auditlogweb.api.AuditLogContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Captures request context for security auditing.
 * And stores IP/User-Agent/session info in {@link AuditLogContext}, and clears the context after the request.
 */
public class AuditContextFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AuditContextFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            HttpSession session = request.getSession(false); 

            AuditLogContext ctx = buildContext(request, session);
            AuditLogContext.set(ctx);
        } catch (Exception e) {
            log.warn("AuditContextFilter: failed to populate SecurityAuditContext", e);
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            AuditLogContext.clear();
        }
    }

    private AuditLogContext buildContext(HttpServletRequest request, HttpSession session) {
        AuditLogContext ctx = new AuditLogContext();
        ctx.setIpAddress(resolveClientIp(request));
        ctx.setUserAgent(request.getHeader("User-Agent"));
        if (session != null) {
            ctx.setSessionId(session.getId());
            ctx.setLoggedInUsername(resolveLoggedInUsername());
        }
        return ctx;
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String resolveLoggedInUsername() {

        if (Context.isAuthenticated() && Context.getAuthenticatedUser() != null) {
            User user = Context.getAuthenticatedUser();
            if (StringUtils.isNotBlank(user.getUsername())) {
                return user.getUsername();
            } else {
                return user.getSystemId();
            }
        }
        return null;
    }
}
