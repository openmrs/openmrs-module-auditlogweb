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
import org.openmrs.module.auditlogweb.AuditSecurityEvent;
import org.openmrs.module.auditlogweb.api.AuditService;
import org.openmrs.module.auditlogweb.api.dto.SecurityAuditLogDTO;
import org.openmrs.module.auditlogweb.api.dto.SecurityLogResponseDTO;
import org.openmrs.module.auditlogweb.api.utils.AuditSecurityEventType;
import org.openmrs.module.auditlogweb.api.utils.UtilClass;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/rest/" + RestConstants.VERSION_1 + "/securityauditlogs")
@RequiredArgsConstructor
public class SecurityAuditRestController {
	
	private final AuditService auditService;
	
	@GetMapping
	public SecurityLogResponseDTO fetchSecurityAudits(@RequestParam(value = "logId", required = false) Integer logId,
	        @RequestParam(value = "eventType", required = false) String eventType,
	        @RequestParam(value = "username", required = false) String username,
	        @RequestParam(value = "startDate", required = false) String startDate,
	        @RequestParam(value = "endDate", required = false) String endDate,
	        @RequestParam(value = "page", defaultValue = "0") int page,
	        @RequestParam(value = "size", defaultValue = "15") int size) {
		
		if (logId != null && logId <= 0) {
			throw new IllegalArgumentException("Please provide a valid log ID");
		}
		
		if (eventType != null && !eventType.trim().isEmpty()) {
			AuditSecurityEventType parsed = AuditSecurityEventType.fromName(eventType);
			if (parsed == null || parsed == AuditSecurityEventType.UNKNOWN) {
				throw new IllegalArgumentException("Invalid eventType " + eventType);
			}
		}
		
		if (logId != null) {
			AuditSecurityEvent securityEvent = auditService.getSecurityEventById(logId);
			if (securityEvent == null) {
				throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No log found for this logId");
			}
			List<SecurityAuditLogDTO> securityAuditLogsDTO = mapToDTOs(Collections.singletonList(securityEvent));
			return SecurityLogResponseDTO.builder().totalLogs(1).currentLogs(1).securityAuditLogs(securityAuditLogsDTO)
			        .totalPages(1).build();
		}
		
		size = UtilClass.sanitizePageSizeValue(size);
		page = UtilClass.sanitizePageValue(page, size);
		
		Date start = UtilClass.parseDate(startDate, false);
		Date end = UtilClass.parseDate(endDate, true);
		
		List<AuditSecurityEvent> securityEvents = auditService.getSecurityEvents(eventType, username, start, end, page,
		    size);
		long totalCount = auditService.countSecurityEvents(eventType, username, start, end);
		int totalPages = UtilClass.computeTotalPages(totalCount, size);
		
		List<SecurityAuditLogDTO> securityAuditLogsDTO = mapToDTOs(securityEvents);
		
		return SecurityLogResponseDTO.builder().totalLogs(totalCount).currentLogs(securityAuditLogsDTO.size())
		        .totalPages(totalPages).currentPage(page).securityAuditLogs(securityAuditLogsDTO).build();
	}
	
	@GetMapping("/relatedAudits")
	public SecurityLogResponseDTO fetchRelatedAudits(@RequestParam(value = "sessionId") String sessionId,
	        @RequestParam(value = "page", defaultValue = "0") int page,
	        @RequestParam(value = "size", defaultValue = "15") int size) {
		
		if (sessionId.trim().isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid session id");
		}
		
		size = UtilClass.sanitizePageSizeValue(size);
		page = UtilClass.sanitizePageValue(page, size);
		
		List<AuditSecurityEvent> allRelated = auditService.getRelatedSecurityEvents(sessionId, page, size);
		long totalCount = auditService.countRelatedSecurityEvents(sessionId);
		int totalPages = UtilClass.computeTotalPages(totalCount, size);
		
		List<SecurityAuditLogDTO> securityAuditLogsDTO = mapToDTOs(allRelated);
		
		return SecurityLogResponseDTO.builder().totalLogs(totalCount).currentLogs(securityAuditLogsDTO.size())
		        .totalPages(totalPages).currentPage(page).securityAuditLogs(securityAuditLogsDTO).build();
	}
	
	private SecurityAuditLogDTO mapToDTO(AuditSecurityEvent event) {
		if (event == null) {
			return null;
		}
		return SecurityAuditLogDTO.builder().id(event.getId()).eventType(event.getEventType()).username(event.getUsername())
		        .userUUID(event.getUserUuid()).eventTime(event.getEventTime()).ipAddress(event.getIpAddress())
		        .userAgent(event.getUserAgent()).sessionId(event.getSessionId()).details(event.getDetails()).build();
	}
	
	private List<SecurityAuditLogDTO> mapToDTOs(List<AuditSecurityEvent> events) {
		if (events == null) {
			return Collections.emptyList();
		}
		List<SecurityAuditLogDTO> dtos = new ArrayList<>(events.size());
		for (AuditSecurityEvent event : events) {
			dtos.add(mapToDTO(event));
		}
		return dtos;
	}
}
