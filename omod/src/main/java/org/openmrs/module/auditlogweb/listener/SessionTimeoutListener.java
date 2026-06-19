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
import org.apache.commons.lang3.StringUtils;
import org.openmrs.api.context.UserContext;
import org.openmrs.module.auditlogweb.api.AuditService;
import org.openmrs.module.auditlogweb.api.PasswordResetFlowContext;
import org.openmrs.module.auditlogweb.api.listener.ExplicitLogoutSessionTracker;
import org.openmrs.module.auditlogweb.api.listener.LoginFixationSessionTracker;
import org.openmrs.module.auditlogweb.api.listener.LogoutListener;
import org.openmrs.module.auditlogweb.api.utils.AuditSecurityEventType;
import org.openmrs.web.WebConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Logs {@code SESSION_TIMEOUT} events when an HTTP session expires.
 * Explicit logouts are skipped because {@link LogoutListener} already records them.
 */
@Component
@RequiredArgsConstructor
public class SessionTimeoutListener implements HttpSessionListener {

    private static final Logger log = LoggerFactory.getLogger(SessionTimeoutListener.class);
    private final AuditService auditService;

    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
        HttpSession session = event.getSession();

        PasswordResetFlowContext.clear(session.getId());

        if (ExplicitLogoutSessionTracker.consume(session.getId())) return;

        // Skip the pre-login session destroyed by login session fixation protection.
        if (LoginFixationSessionTracker.consume(session.getId())) return;

        String username = resolveUsername(session);
        if (StringUtils.isBlank(username)) return;

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

    // Fetch the username the user context.
    private String resolveUsername(HttpSession session) {
        try {
            Object raw = session.getAttribute(WebConstants.OPENMRS_USER_CONTEXT_HTTPSESSION_ATTR);
            if (raw instanceof UserContext) {
                UserContext userContext = (UserContext) raw;
                if (userContext.getAuthenticatedUser() != null) {
                    String username = userContext.getAuthenticatedUser().getUsername();
                    if (username == null || username.isEmpty()) {
                        username = userContext.getAuthenticatedUser().getSystemId();
                    }
                    return username;
                }
            }
        } catch (Exception e) {
            log.error("Could not read UserContext from session", e);
        }

        return null;
    }

}
