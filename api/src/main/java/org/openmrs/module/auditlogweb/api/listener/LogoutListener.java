/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.auditlogweb.api.listener;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.openmrs.User;
import org.openmrs.UserSessionListener;
import org.openmrs.module.auditlogweb.api.AuditService;
import org.openmrs.module.auditlogweb.api.AuditLogContext;
import org.openmrs.module.auditlogweb.api.utils.AuditSecurityEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LogoutListener implements UserSessionListener {
	
	private final Logger log = LoggerFactory.getLogger(LogoutListener.class);
	
	private final AuditService auditService;
	
	@Override
	public void loggedInOrOut(User user, Event event, Status status) {
		try {
			
			if (event != Event.LOGOUT) {
				return;
			}
			
			AuditLogContext ctx = AuditLogContext.get();
			String ipAddress = ctx != null ? ctx.getIpAddress() : null;
			String userAgent = ctx != null ? ctx.getUserAgent() : null;
			String sessionId = ctx != null ? ctx.getSessionId() : null;
			String username = ctx != null ? ctx.getLoggedInUsername() : null;
			if (StringUtils.isBlank(username) && user != null) {
				username = user.getUsername() != null ? user.getUsername() : user.getSystemId();
			}
			
			log.debug("LogoutListener: logging LOGOUT for user [{}]", username);
			
			if (auditService == null) {
				log.warn("SecurityEventListener: AuditService is not registered, skipping logout audit event");
				return;
			}
			
			auditService.logSecurityEvent(AuditSecurityEventType.LOGOUT, username, user != null ? user.getUuid() : null,
			    ipAddress, userAgent, sessionId, null);
			log.info("Log out event saved ");
			// Marking the session so SessionTimeoutListener knows this was an explicit logout,
			// not a timeout when the container later calls #sessionDestroyed.
			markSessionAsExplicitLogout(ctx);
		}
		catch (Exception e) {
			log.error("Failed to log logout event", e);
		}
	}
	
	/**
	 * Marks the session as explicitly logged out. This will be used by the SessionTimeoutListener in
	 * omod to determine whether the logout was explicit or not.
	 *
	 * @param ctx the security audit context
	 */
	private void markSessionAsExplicitLogout(AuditLogContext ctx) {
		String sessionId = ctx != null ? ctx.getSessionId() : null;
		if (sessionId == null) {
			return;
		}
		ExplicitLogoutSessionTracker.mark(sessionId);
	}
}
