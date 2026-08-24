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
import org.openmrs.module.auditlogweb.ReadAuditEntityMetadata;
import org.openmrs.module.auditlogweb.ReadAuditLog;
import org.openmrs.module.auditlogweb.api.ReadAuditService;
import org.openmrs.module.auditlogweb.api.dao.ReadAuditDAO;
import org.openmrs.module.auditlogweb.api.dto.ReadAuditEntityMetadataDTO;
import org.openmrs.module.auditlogweb.api.dto.ReadAuditLogDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@RequiredArgsConstructor
public class ReadAuditServiceImpl extends BaseOpenmrsService implements ReadAuditService {
	
	private final ReadAuditDAO readAuditDAO;
	
	@Override
	@Transactional(readOnly = true)
	public List<ReadAuditLog> getReadAuditLogs(String eventType, String username, Date startDate, Date endDate, int page,
	        int size) {
		return readAuditDAO.getReadAuditLogs(eventType, username, startDate, endDate, page, size);
	}
	
	@Override
	@Transactional(readOnly = true)
	public long countReadAuditLogs(String eventType, String username, Date startDate, Date endDate) {
		return readAuditDAO.countReadAuditLogs(eventType, username, startDate, endDate);
	}
	
	@Override
	public ReadAuditLog getReadAuditLogById(Integer id) {
		return readAuditDAO.getReadAuditLogById(id);
	}
	
	@Override
	public List<ReadAuditLog> getRelatedReadLogs(String sessionId, int page, int size) {
		return readAuditDAO.getRelatedReadLogs(sessionId, page, size);
	}
	
	@Override
	public long countRelatedReadLogs(String sessionId) {
		return readAuditDAO.countRelatedReadLogs(sessionId);
	}
	
	@Override
	public List<String> getEntityTypes() {
		return readAuditDAO.getEntityTypes();
	}
	
	@Override
	public List<ReadAuditLogDTO> mapToReadAuditLogDTO(List<ReadAuditLog> readAuditLogs) {
		
		List<ReadAuditLogDTO> readAuditLogDTOs = new ArrayList<>();
		
		for (ReadAuditLog currAudit : readAuditLogs) {
			ReadAuditLogDTO readAuditLogDTO = ReadAuditLogDTO.builder().id(currAudit.getId())
			        .entityName(currAudit.getEntityName())
			        .entityMetadata(mapToReadAuditEntityMetadataDTO(currAudit.getTargets()))
			        .isReadSuccess(currAudit.isReadSuccess()).username(currAudit.getUsername())
			        .userUUID(currAudit.getUserUUID()).eventTime(currAudit.getEventTime())
			        .ipAddress(currAudit.getIpAddress()).userAgent(currAudit.getUserAgent())
			        .sessionId(currAudit.getSessionId()).build();
			readAuditLogDTOs.add(readAuditLogDTO);
		}
		
		return readAuditLogDTOs;
	}
	
	private List<ReadAuditEntityMetadataDTO> mapToReadAuditEntityMetadataDTO(
	        List<ReadAuditEntityMetadata> entityMetadataList) {
		
		List<ReadAuditEntityMetadataDTO> readAuditEntityMetadataDTOs = new ArrayList<>();
		for (ReadAuditEntityMetadata currEntityMetadata : entityMetadataList) {
			ReadAuditEntityMetadataDTO metadataDTO = ReadAuditEntityMetadataDTO.builder().id(currEntityMetadata.getId())
			        .entityUUID(currEntityMetadata.getEntityUuid()).patientUUID(currEntityMetadata.getPatientUuid())
			        .patientName(currEntityMetadata.getPatientName()).build();
			readAuditEntityMetadataDTOs.add(metadataDTO);
		}
		return readAuditEntityMetadataDTOs;
	}
}
