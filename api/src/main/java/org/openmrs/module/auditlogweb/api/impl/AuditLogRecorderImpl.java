/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.auditlogweb.api.impl;

import lombok.RequiredArgsConstructor;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.Daemon;
import org.openmrs.module.auditlogweb.api.utils.ModuleEventType;
import org.openmrs.module.auditlogweb.ModuleEvent;
import org.openmrs.module.auditlogweb.ReadAuditLog;
import org.openmrs.module.auditlogweb.api.AuditLogContext;
import org.openmrs.module.auditlogweb.api.AuditLogRecorder;
import org.openmrs.module.auditlogweb.api.dao.ModuleEventDao;
import org.openmrs.module.auditlogweb.api.dao.ReadAuditDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Component("auditlogweb.AuditLogRecorder")
@RequiredArgsConstructor
public class AuditLogRecorderImpl implements AuditLogRecorder {
	
	private final Logger log = LoggerFactory.getLogger(AuditLogRecorderImpl.class);
	
	private final ReadAuditDAO readAuditDAO;
	
	private final ModuleEventDao moduleEventDao;
	
	@Override
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void logReadAudit(ReadAuditLog readAuditLog) {
		readAuditDAO.saveReadAuditLog(readAuditLog);
	}
	
	@Override
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void logReadAudits(List<ReadAuditLog> readAuditLogs) {
		if (readAuditLogs != null) {
			for (ReadAuditLog log : readAuditLogs) {
				readAuditDAO.saveReadAuditLog(log);
			}
		}
	}
	
	@Override
	public void logModuleEvent(ModuleEventType moduleEventType, String moduleId, String moduleName, String moduleVersion,
	        boolean isSuccess, String failureReason) {
		String username = null;
		String userUUID = null;
		String ipAddress = null;
		String userAgent = null;
		String sessionId = null;
		
		try {
			AuditLogContext ctx = AuditLogContext.get();
			if (ctx != null) {
				username = ctx.getLoggedInUsername();
				userUUID = ctx.getLoggedInUserUUID();
				ipAddress = ctx.getIpAddress();
				userAgent = ctx.getUserAgent();
				if (userAgent != null && userAgent.length() > 500) {
					userAgent = userAgent.substring(0, 500);
				}
				sessionId = ctx.getSessionId();
			}
			
			if (userUUID == null) {
				if (Context.isAuthenticated()) {
					User user = Context.getAuthenticatedUser();
					if (user != null && Daemon.isDaemonUser(user)) {
						return;
					}
					if (user != null) {
						username = user.getUsername();
						if (username == null) {
							username = user.getSystemId();
						}
						userUUID = user.getUuid();
					}
				}
			}
			
			if (userUUID == null) {
				userUUID = "anonymous";
				username = "anonymous";
			}
			
			if (moduleEventType == null) {
				log.warn("Module event type can't be null");
				return;
			}
			
			if (failureReason != null && failureReason.length() > 500) {
				failureReason = failureReason.substring(0, 500);
			}
			
			ModuleEvent moduleEvent = ModuleEvent.builder().eventType(moduleEventType).moduleId(moduleId)
			        .moduleName(moduleName).moduleVersion(moduleVersion).eventSuccess(isSuccess).username(username)
			        .userUUID(userUUID).eventTime(new Date()).ipAddress(ipAddress).userAgent(userAgent).sessionId(sessionId)
			        .failureReason(failureReason).build();
			logModuleEvent(moduleEvent);
		}
		catch (Exception e) {
			log.error("Error while saving module event", e);
		}
	}
	
	@Override
	public void logModuleEvent(ModuleEvent moduleEvent) {
		moduleEventDao.saveModuleEvent(moduleEvent);
	}
	
}
