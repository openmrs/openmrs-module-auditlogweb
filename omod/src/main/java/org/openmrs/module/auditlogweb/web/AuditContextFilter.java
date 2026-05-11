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

import org.openmrs.api.context.Context;
import org.apache.commons.lang3.StringUtils;
import org.openmrs.module.auditlogweb.AuditlogwebConstants;
import org.openmrs.module.auditlogweb.api.SecurityAuditContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Captures request context for security auditing.
 * Stores IP/User-Agent/session info in {@link SecurityAuditContext}, keeps the raw
 * {@link HttpSession} available to omod components, stamps LOGGED_IN_USER,
 * and clears both ThreadLocals after the request.
 */
public class AuditContextFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AuditContextFilter.class);

    private static final ThreadLocal<HttpSession> SESSION_HOLDER = new ThreadLocal<>();

    public static HttpSession getCurrentSession() {
        return SESSION_HOLDER.get();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain filterChain)
            throws ServletException, IOException {

        try {
            HttpSession session = request.getSession(false); 
            SESSION_HOLDER.set(session);

            SecurityAuditContext ctx = buildContext(request, session);
            SecurityAuditContext.set(ctx);
            // log.debug("Going to stamp the logger user from filter");
            stampLoggedInUser(session);

        } catch (Exception e) {
            log.warn("AuditContextFilter: failed to populate SecurityAuditContext", e);
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            SESSION_HOLDER.remove();
            SecurityAuditContext.clear();
        }
    }


    private SecurityAuditContext buildContext(HttpServletRequest request, HttpSession session) {
        SecurityAuditContext ctx = new SecurityAuditContext();
        ctx.setIpAddress(resolveClientIp(request));
        ctx.setUserAgent(request.getHeader("User-Agent"));
        if (session != null) {
            ctx.setSessionId(session.getId());
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

    private void stampLoggedInUser(HttpSession session) {
        if (session == null) {
            return;
        }
        log.debug("session id when stamping logged in user in filter {}", session.getId());

        try {
            
            if (session.getAttribute(AuditlogwebConstants.SESSION_ATTR_LOGGED_IN_USER) != null) {
                log.debug("Logged username already there on session ");
                return; 
            }
            if (Context.isAuthenticated()) {
                String username = Context.getAuthenticatedUser().getUsername();
                if (!StringUtils.isBlank(username)) {
                    session.setAttribute(AuditlogwebConstants.SESSION_ATTR_LOGGED_IN_USER, username);
                    log.info("Logged in username set into the session now");
                }
            }
        } catch (Exception e) {
            log.warn("Could not stamp LOGGED_IN_USER on session", e);
        }
    }
}
