/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.auditlogweb.api.dao;

import org.openmrs.module.auditlogweb.api.utils.ModuleEventType;
import org.openmrs.module.auditlogweb.ModuleEvent;

import java.util.Date;
import java.util.List;

public interface ModuleEventDao {
	
	void saveModuleEvent(ModuleEvent moduleEvent);
	
	List<ModuleEvent> getModuleEvents(ModuleEventType eventType, String moduleId, String moduleName, String moduleVersion,
	        String username, String userUUID, Date startDate, Date endDate, int page, int size);
	
	Long countModuleEvents(ModuleEventType eventType, String moduleId, String moduleName, String moduleVersion,
	        String username, String userUUID, Date startDate, Date endDate);
	
	ModuleEvent getModuleEventById(Integer moduleEventId);
	
	List<ModuleEvent> getRelatedModuleEvents(String sessionId, int page, int size);
	
	Long countRelatedModuleEvents(String sessionId);
}
