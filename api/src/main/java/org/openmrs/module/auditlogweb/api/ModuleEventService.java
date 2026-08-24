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
import org.openmrs.api.OpenmrsService;
import org.openmrs.module.auditlogweb.ModuleEvent;
import org.openmrs.module.auditlogweb.api.dto.ModuleEventDTO;
import org.openmrs.module.auditlogweb.api.utils.AuditLogConstants;

import java.util.Date;
import java.util.List;

public interface ModuleEventService extends OpenmrsService {
	
	@Authorized(AuditLogConstants.VIEW_ADMIN_AUDIT_LOGS)
	List<ModuleEvent> getModuleEvents(String eventType, String moduleId, String moduleName, String moduleVersion,
	        String username, String userUUID, Date startDate, Date endDate, int page, int size);
	
	@Authorized(AuditLogConstants.VIEW_ADMIN_AUDIT_LOGS)
	long countModuleEvents(String eventType, String moduleId, String moduleName, String moduleVersion, String username,
	        String userUUID, Date start, Date end);
	
	@Authorized(AuditLogConstants.VIEW_ADMIN_AUDIT_LOGS)
	ModuleEvent getModuleEventById(Integer moduleEventId);
	
	@Authorized(AuditLogConstants.VIEW_ADMIN_AUDIT_LOGS)
	List<ModuleEvent> getRelatedModuleEvents(String sessionId, int page, int size);
	
	@Authorized(AuditLogConstants.VIEW_ADMIN_AUDIT_LOGS)
	long countRelatedModuleEvents(String sessionId);
	
	List<ModuleEventDTO> mapToModuleEventDTO(List<ModuleEvent> moduleEvents);
}
