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
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.auditlogweb.api.utils.ModuleEventType;
import org.openmrs.module.auditlogweb.ModuleEvent;
import org.openmrs.module.auditlogweb.api.ModuleEventService;
import org.openmrs.module.auditlogweb.api.dao.ModuleEventDao;
import org.openmrs.module.auditlogweb.api.dto.ModuleEventDTO;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@RequiredArgsConstructor
public class ModuleEventServiceImpl extends BaseOpenmrsService implements ModuleEventService {
	
	private final ModuleEventDao moduleEventDao;
	
	@Override
	public List<ModuleEvent> getModuleEvents(String eventType, String moduleId, String moduleName, String moduleVersion,
	        String username, String userUUID, Date startDate, Date endDate, int page, int size) {
		return moduleEventDao.getModuleEvents(getModuleEventType(eventType), moduleId, moduleName, moduleVersion, username,
		    userUUID, startDate, endDate, page, size);
	}
	
	@Override
	public long countModuleEvents(String eventType, String moduleId, String moduleName, String moduleVersion,
	        String username, String userUUID, Date startDate, Date endDate) {
		return moduleEventDao.countModuleEvents(getModuleEventType(eventType), moduleId, moduleName, moduleVersion, username,
		    userUUID, startDate, endDate);
	}
	
	@Override
	public ModuleEvent getModuleEventById(Integer moduleEventId) {
		return moduleEventDao.getModuleEventById(moduleEventId);
	}
	
	@Override
	public List<ModuleEvent> getRelatedModuleEvents(String sessionId, int page, int size) {
		return moduleEventDao.getRelatedModuleEvents(sessionId, page, size);
	}
	
	@Override
	public long countRelatedModuleEvents(String sessionId) {
		return moduleEventDao.countRelatedModuleEvents(sessionId);
	}
	
	@Override
	public List<ModuleEventDTO> mapToModuleEventDTO(List<ModuleEvent> moduleEvents) {
		List<ModuleEventDTO> moduleEventDTOs = new ArrayList<>();
		
		for (ModuleEvent moduleEvent : moduleEvents) {
			ModuleEventDTO dto = ModuleEventDTO.builder().id(moduleEvent.getId()).eventType(moduleEvent.getEventType())
			        .moduleId(moduleEvent.getModuleId()).moduleName(moduleEvent.getModuleName())
			        .moduleVersion(moduleEvent.getModuleVersion()).eventSuccess(moduleEvent.isEventSuccess())
			        .username(moduleEvent.getUsername()).userUUID(moduleEvent.getUserUUID())
			        .eventTime(moduleEvent.getEventTime()).ipAddress(moduleEvent.getIpAddress())
			        .userAgent(moduleEvent.getUserAgent()).sessionId(moduleEvent.getSessionId())
			        .failureReason(moduleEvent.getFailureReason()).build();
			moduleEventDTOs.add(dto);
		}
		return moduleEventDTOs;
	}
	
	private ModuleEventType getModuleEventType(String eventType) {
		ModuleEventType moduleEventType = null;
		if (eventType != null && !eventType.isEmpty()) {
			try {
				moduleEventType = ModuleEventType.valueOf(eventType.trim().toUpperCase());
			}
			catch (IllegalArgumentException e) {
				throw new IllegalArgumentException("Invalid event type: " + eventType);
			}
		}
		return moduleEventType;
	}
}
