/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.auditlogweb.api;

import org.openmrs.annotation.Authorized;
import org.openmrs.module.auditlogweb.ReadAuditLog;
import org.openmrs.module.auditlogweb.api.utils.AuditLogConstants;

import org.aspectj.lang.ProceedingJoinPoint;

import java.util.Date;
import java.util.List;

public interface ReadAuditService {

	void logReadAudit(ReadAuditLog readAuditLog);

	void logReadAudits(List<ReadAuditLog> readAuditLogs);

	@Authorized(AuditLogConstants.VIEW_READ_AUDIT_LOGS)
	List<ReadAuditLog> getReadAuditLogs(String eventType, String username, Date startDate, Date endDate, int page, int size);

	@Authorized(AuditLogConstants.VIEW_READ_AUDIT_LOGS)
	long countReadAuditLogs(String eventType, String username, Date startDate, Date endDate);

	@Authorized(AuditLogConstants.VIEW_READ_AUDIT_LOGS)
	ReadAuditLog getReadAuditLogById(Integer id);

	@Authorized(AuditLogConstants.VIEW_READ_AUDIT_LOGS)
	List<ReadAuditLog> getRelatedReadLogs(String sessionId, int limit);

	Object auditReadRequest(ProceedingJoinPoint joinPoint) throws Throwable;

	void saveReadAuditRequest(String entityName, boolean isReadSuccess, Object result);

}
