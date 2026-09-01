/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.auditlogweb.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;
import org.openmrs.module.auditlogweb.api.utils.AuditSecurityEventType;

import java.util.Date;

@Data
@Builder
public class SecurityAuditLogDTO {
	
	private Integer id;
	
	private AuditSecurityEventType eventType;
	
	private String username;
	
	private String userUUID;
	
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy HH:mm:ss", timezone = "GMT")
	private Date eventTime;
	
	private String ipAddress;
	
	private String userAgent;
	
	private String sessionId;
	
	private String details;
	
}
