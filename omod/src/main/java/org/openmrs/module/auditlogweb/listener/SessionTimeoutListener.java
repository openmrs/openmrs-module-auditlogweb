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

import lombok.RequiredArgsConstructor;
import org.openmrs.api.context.UserContext;
import org.openmrs.module.auditlogweb.AuditlogwebConstants;
import org.openmrs.module.auditlogweb.api.AuditService;
import org.openmrs.module.auditlogweb.api.utils.AuditSecurityEventType;
import org.openmrs.web.WebConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Logs {@code SESSION_TIMEOUT} events when an HTTP session expires.
 * Explicit logouts are skipped because {@link SecurityEventListener} already records them.
 */
@Component
@RequiredArgsConstructor
public class SessionTimeoutListener implements HttpSessionListener {

    private static final Logger log = LoggerFactory.getLogger(SessionTimeoutListener.class);
    private final AuditService auditService;

    @Override
    public void sessionCreated(HttpSessionEvent event) {
        // No action needed on session creation.
        log.debug("Session created id {}", event.getSession().getId());
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
        HttpSession session = event.getSession();
        log.debug("Session destroyed id {}", session.getId());

        if (Boolean.TRUE.equals(session.getAttribute(AuditlogwebConstants.SESSION_ATTR_EXPLICIT_LOGOUT))) {
            log.debug("Session destroyed after explicit logout, skipping it");
            return;
        } else {
            log.debug("Session not destroyed because of explicit logout");
        }

        // Skip the pre-login session destroyed by login session fixation protection.
        if (LoginFixationSessionTracker.consume(session.getId())) {
            log.debug("Session destroyed due to session fixation on login, skipping SESSION_TIMEOUT");
            return;
        }

        String username = resolveUsername(session);
        if (username == null) {
            log.debug("Session destroyed but no authenticated user found, skipping it");
            return;
        } else {
            log.debug("Session destroyed but authenticated user found [{}]", username);
        }

        try {
            auditService.logSecurityEvent(
                    AuditSecurityEventType.SESSION_TIMEOUT,
                    username,
                    null,
                    null,
                    null,
                    session.getId(),
                    null);
        } catch (Exception e) {
            log.error("Failed to log SESSION_TIMEOUT for user [{}]", username, e);
        }
    }

    // Fetch the username either from the session or from the user context as fallback.
    private String resolveUsername(HttpSession session) {
        log.debug("Called the resolveUsername method for session [{}]", session.getId());
        String username = "";
        try {
            log.debug("Session id before getting user context {}", session.getId());
            Object raw = session.getAttribute(WebConstants.OPENMRS_USER_CONTEXT_HTTPSESSION_ATTR);
            if (raw instanceof UserContext) {
                UserContext userContext = (UserContext) raw;
                if (userContext.getAuthenticatedUser() != null) {
                    username = userContext.getAuthenticatedUser().getUsername();
                    if (username == null || username.isEmpty()) {
                        username = userContext.getAuthenticatedUser().getSystemId();
                        log.debug("Username found from UserContext [{}]", username);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Could not read UserContext from session", e);
        }

        return username;
    }

}
