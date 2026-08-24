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

import lombok.Builder;
import lombok.Data;
import org.openmrs.module.auditlogweb.api.utils.ModuleEventType;

import java.util.Date;

@Data
@Builder
public class ModuleEventDTO {
	
	private Integer id;
	
	private ModuleEventType eventType;
	
	private String moduleId;
	
	private String moduleName;
	
	private String moduleVersion;
	
	private boolean eventSuccess;
	
	private String failureReason;
	
	private String username;
	
	private String userUUID;
	
	private Date eventTime;
	
	private String ipAddress;
	
	private String userAgent;
	
	private String sessionId;
}
