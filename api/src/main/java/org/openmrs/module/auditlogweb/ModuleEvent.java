/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.auditlogweb;

import lombok.Getter;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.openmrs.module.auditlogweb.api.utils.ModuleEventType;
import javax.persistence.Id;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Column;
import javax.persistence.Enumerated;
import javax.persistence.EnumType;
import java.util.Date;

@Entity
@Table(name = "module_event_audit")
@Builder
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ModuleEvent {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;
	
	@Column(name = "event_type", nullable = false)
	@Enumerated(EnumType.STRING)
	private ModuleEventType eventType;
	
	@Column(name = "module_id")
	private String moduleId;
	
	@Column(name = "module_name", nullable = false)
	private String moduleName;
	
	@Column(name = "module_version")
	private String moduleVersion;
	
	@Column(name = "event_success", nullable = false)
	private boolean eventSuccess;
	
	@Column(name = "failure_reason")
	private String failureReason;
	
	@Column(name = "username", length = 50)
	private String username;
	
	@Column(name = "user_uuid", length = 38)
	private String userUUID;
	
	@Column(name = "event_time", nullable = false)
	private Date eventTime;
	
	@Column(name = "ip_address", length = 100)
	private String ipAddress;
	
	@Column(name = "user_agent", length = 500)
	private String userAgent;
	
	@Column(name = "session_id", length = 256)
	private String sessionId;
}
