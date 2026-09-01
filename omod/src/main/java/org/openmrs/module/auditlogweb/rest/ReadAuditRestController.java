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
import org.openmrs.module.auditlogweb.ReadAuditLog;
import org.openmrs.module.auditlogweb.api.ReadAuditService;
import org.openmrs.module.auditlogweb.api.dto.ReadAuditLogResponseDTO;
import org.openmrs.module.auditlogweb.api.utils.AuditLogConstants;
import org.openmrs.module.auditlogweb.api.utils.UtilClass;
import org.openmrs.module.auditlogweb.api.dto.ReadAuditLogDTO;
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
@RequestMapping("/rest/" + RestConstants.VERSION_1 + "/readauditlogs")
@RequiredArgsConstructor
public class ReadAuditRestController {
	
	private final ReadAuditService readAuditService;
	
	@GetMapping
	public ReadAuditLogResponseDTO fetchReadAudits(@RequestParam(value = "logId", required = false) Integer logId,
	        @RequestParam(value = "entityName", required = false) String entityName,
	        @RequestParam(value = "username", required = false) String username,
	        @RequestParam(value = "startDate", required = false) String startDate,
	        @RequestParam(value = "endDate", required = false) String endDate,
	        @RequestParam(value = "page", defaultValue = "0") int page,
	        @RequestParam(value = "size", defaultValue = "15") int size) {
		
		if (logId != null && logId <= 0) {
			throw new IllegalArgumentException("Please provide a valid log ID");
		}
		
		if (logId != null) {
			ReadAuditLog readAuditLog = readAuditService.getReadAuditLogById(logId);
			if (readAuditLog == null) {
				throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No log found for this logId");
			}
			List<ReadAuditLogDTO> readAuditLogsDTO = readAuditService
			        .mapToReadAuditLogDTO(Collections.singletonList(readAuditLog));
			return ReadAuditLogResponseDTO.builder().totalLogs(1).currentLogs(1).readAuditLogs(readAuditLogsDTO)
			        .totalPages(1).build();
		}
		
		size = UtilClass.sanitizePageSizeValue(size);
		page = UtilClass.sanitizePageValue(page, size);
		
		Date start = UtilClass.parseDate(startDate, false);
		Date end = UtilClass.parseDate(endDate, true);
		
		List<ReadAuditLog> readAuditLogs = readAuditService.getReadAuditLogs(entityName, username, start, end, page, size);
		long totalCount = readAuditService.countReadAuditLogs(entityName, username, start, end);
		int totalPages = UtilClass.computeTotalPages(totalCount, size);
		
		List<ReadAuditLogDTO> readAuditLogsDTO = readAuditService.mapToReadAuditLogDTO(readAuditLogs);
		
		return ReadAuditLogResponseDTO.builder().totalLogs(totalCount)
		        .currentLogs(readAuditLogsDTO != null ? readAuditLogsDTO.size() : 0).totalPages(totalPages).currentPage(page)
		        .readAuditLogs(readAuditLogsDTO).build();
		
	}
	
	@GetMapping("/relatedAudits")
	public ReadAuditLogResponseDTO fetchRelatedAudits(@RequestParam(value = "sessionId") String sessionId,
	        @RequestParam(value = "page", defaultValue = "0") int page,
	        @RequestParam(value = "size", defaultValue = "15") int size) {
		
		if (sessionId.trim().isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid session id");
		}
		
		size = UtilClass.sanitizePageSizeValue(size);
		page = UtilClass.sanitizePageValue(page, size);
		
		List<ReadAuditLog> relatedAudits = readAuditService.getRelatedReadLogs(sessionId.trim(), page, size);
		long totalCount = readAuditService.countRelatedReadLogs(sessionId);
		int totalPages = UtilClass.computeTotalPages(totalCount, size);
		
		List<ReadAuditLogDTO> readAuditLogsDTO = readAuditService.mapToReadAuditLogDTO(relatedAudits);
		
		return ReadAuditLogResponseDTO.builder().totalLogs(totalCount)
		        .currentLogs(readAuditLogsDTO != null ? readAuditLogsDTO.size() : 0).totalPages(totalPages).currentPage(page)
		        .readAuditLogs(readAuditLogsDTO).build();
		
	}
}
