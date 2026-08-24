/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.auditlogweb.rest;

import lombok.RequiredArgsConstructor;
import org.openmrs.module.auditlogweb.ModuleEvent;
import org.openmrs.module.auditlogweb.api.ModuleEventService;
import org.openmrs.module.auditlogweb.api.dto.ModuleEventDTO;
import org.openmrs.module.auditlogweb.api.dto.ModuleEventResponseDTO;
import org.openmrs.module.auditlogweb.api.utils.UtilClass;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/rest/" + RestConstants.VERSION_1 + "/moduleactionlogs")
@RequiredArgsConstructor
public class ModuleEventRestController {
	
	private final ModuleEventService moduleEventService;
	
	@GetMapping
	public ModuleEventResponseDTO fetchModuleEvents(@RequestParam(value = "logId", required = false) Integer logId,
	        @RequestParam(value = "eventType", required = false) String eventType,
	        @RequestParam(value = "moduleId", required = false) String moduleId,
	        @RequestParam(value = "moduleName", required = false) String moduleName,
	        @RequestParam(value = "moduleVersion", required = false) String moduleVersion,
	        @RequestParam(value = "username", required = false) String username,
	        @RequestParam(value = "userUUID", required = false) String userUUID,
	        @RequestParam(value = "startDate", required = false) String startDate,
	        @RequestParam(value = "endDate", required = false) String endDate,
	        @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
	        @RequestParam(value = "size", required = false, defaultValue = "15") Integer size) {
		
		if (logId != null && logId <= 0) {
			throw new IllegalArgumentException("Please provide a valid log ID");
		}
		
		if (logId != null) {
			ModuleEvent moduleEventLog = moduleEventService.getModuleEventById(logId);
			if (moduleEventLog == null) {
				return ModuleEventResponseDTO.builder().totalLogs(0).currentLogs(0).moduleEventLogs(Collections.emptyList())
				        .build();
			}
			
			List<ModuleEventDTO> moduleEventDTO = moduleEventService
			        .mapToModuleEventDTO(Collections.singletonList(moduleEventLog));
			return ModuleEventResponseDTO.builder().totalLogs(1).currentLogs(1).moduleEventLogs(moduleEventDTO).build();
		}
		
		if (page < 0) {
			page = 0;
		}
		if (size <= 0) {
			size = 15;
		}
		
		Date start = UtilClass.parseDate(startDate, false);
		Date end = UtilClass.parseDate(endDate, true);
		
		List<ModuleEvent> moduleEventLogs = moduleEventService.getModuleEvents(eventType, moduleId, moduleName,
		    moduleVersion, username, userUUID, start, end, page, size);
		long totalLogs = moduleEventService.countModuleEvents(eventType, moduleId, moduleName, moduleVersion, username,
		    userUUID, start, end);
		int totalPages = UtilClass.computeTotalPages(totalLogs, size);
		
		List<ModuleEventDTO> moduleEventDTO = moduleEventService.mapToModuleEventDTO(moduleEventLogs);
		
		return ModuleEventResponseDTO.builder().totalLogs(totalLogs)
		        .currentLogs(moduleEventDTO != null ? moduleEventDTO.size() : 0).totalPages(totalPages).currentPage(page)
		        .moduleEventLogs(moduleEventDTO).build();
		
	}
	
	@GetMapping("/releatedAudits")
	public ModuleEventResponseDTO fetchRelatedAudits(@RequestParam(value = "sessionId") String sessionId,
	        @RequestParam(value = "page", defaultValue = "0") int page,
	        @RequestParam(value = "size", defaultValue = "15") int size) {
		
		if (sessionId == null || sessionId.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid session id");
		}
		
		if (page < 0) {
			page = 0;
		}
		if (size <= 0) {
			size = 15;
		}
		
		List<ModuleEvent> relatedAudits = moduleEventService.getRelatedModuleEvents(sessionId, page, size);
		long totalCount = moduleEventService.countRelatedModuleEvents(sessionId);
		int totalPages = UtilClass.computeTotalPages(totalCount, size);
		
		List<ModuleEventDTO> readAuditLogsDTO = moduleEventService.mapToModuleEventDTO(relatedAudits);
		
		return ModuleEventResponseDTO.builder().totalLogs(totalCount)
		        .currentLogs(readAuditLogsDTO != null ? readAuditLogsDTO.size() : 0).totalPages(totalPages).currentPage(page)
		        .currentPage(page).moduleEventLogs(readAuditLogsDTO).build();
		
	}
}
