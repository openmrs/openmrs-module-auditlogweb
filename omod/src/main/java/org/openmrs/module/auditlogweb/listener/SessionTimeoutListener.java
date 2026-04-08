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
import javax.servlet.http.HttpSessionListener;

import java.util.List;

import org.openmrs.api.context.Context;
import org.openmrs.module.auditlogweb.AuditSecurityEventType;
import org.openmrs.module.auditlogweb.api.AuditService;
import org.openmrs.module.auditlogweb.web.AuditContextFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logs {@code SESSION_TIMEOUT} events when an HTTP session expires.
 * Explicit logouts are skipped because {@link SecurityEventListener} already records them.
 */
public class SessionTimeoutListener implements HttpSessionListener {

    private static final Logger log = LoggerFactory.getLogger(SessionTimeoutListener.class);

    @Override
    public void sessionCreated(HttpSessionEvent event) {
        // No action needed on session creation.
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
        HttpSession session = event.getSession();

        // Skip if this was an explicit logout — SecurityEventListener already recorded
        // the LOGOUT row and stamped this marker before the session was invalidated.
        if (Boolean.TRUE.equals(session.getAttribute(SecurityEventListener.SESSION_ATTR_EXPLICIT_LOGOUT))) {
            log.debug("SessionTimeoutListener: session [{}] destroyed after explicit logout — skipping", session.getId());
            return;
        }

        // If LOGGED_IN_USER is not set this was an anonymous / unauthenticated session.
        String username = (String) session.getAttribute(AuditContextFilter.SESSION_ATTR_LOGGED_IN_USER);
        if (username == null) {
            log.debug("SessionTimeoutListener: session [{}] destroyed but no LOGGED_IN_USER found — skipping", session.getId());
            return;
        }

        log.debug("SessionTimeoutListener: logging SESSION_TIMEOUT for user [{}], session [{}]", username, session.getId());

        try {
            AuditService auditService = resolveAuditService();
            if (auditService == null) {
                log.warn("SessionTimeoutListener: AuditService is not registered, skipping timeout audit event");
                return;
            }

            //Some parameters are not available due to session got destroyed
            auditService.logSecurityEvent(
                    AuditSecurityEventType.SESSION_TIMEOUT,
                    username,
                    null,         
                    null,        
                    null,        
                    session.getId(),
                    null);
        } catch (Exception e) {
            log.error("SessionTimeoutListener: failed to log SESSION_TIMEOUT for user [{}]", username, e);
        }
    }

    private AuditService resolveAuditService() {
        List<AuditService> auditServices = Context.getRegisteredComponents(AuditService.class);
        return auditServices.isEmpty() ? null : auditServices.get(0);
    }
}
