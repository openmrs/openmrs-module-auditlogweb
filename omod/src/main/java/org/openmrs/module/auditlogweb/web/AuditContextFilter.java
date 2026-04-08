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

    public static final String SESSION_ATTR_LOGGED_IN_USER = "LOGGED_IN_USER";

    private static final ThreadLocal<HttpSession> SESSION_HOLDER = new ThreadLocal<>();

    public static HttpSession getCurrentSession() {
        return SESSION_HOLDER.get();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        try {
            HttpSession session = request.getSession(false); 
            SESSION_HOLDER.set(session);                     

            SecurityAuditContext ctx = buildContext(request, session);
            SecurityAuditContext.set(ctx);
            log.info("Going to stamp the logger user from filter");
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
            log.info("Session is null , getting back");
            return;
        }
        try {
            
            if (session.getAttribute(SESSION_ATTR_LOGGED_IN_USER) != null) {
                log.info("Logged username already there on session ");
                return; 
            }
            if (Context.isAuthenticated()) {
                log.info("Inside the stamp loggerin user, user us authenticated");
                String username = Context.getAuthenticatedUser().getUsername();
                // if (StringUtils.isBlank(username)) {
                //     username = Context.getAuthenticatedUser().getSystemId();
                // }
                log.info("Username after the authentication : "+username);
                if (!StringUtils.isBlank(username)) {
                    session.setAttribute(SESSION_ATTR_LOGGED_IN_USER, username);
                    log.info("logged in username set into the session now");
                }
            }
            log.info("Reached here , means the session is not null nor may be authenticated or have logged in user value ");
        } catch (Exception e) {
            log.debug("AuditContextFilter: could not stamp LOGGED_IN_USER on session", e);
        }
    }
}
