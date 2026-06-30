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

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.AccessLevel;
import org.openmrs.BaseOpenmrsObject;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Column;
import javax.persistence.OneToMany;
import javax.persistence.CascadeType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "read_audit_log")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ReadAuditLog extends BaseOpenmrsObject {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;
	
	@Column(name = "entity_name", nullable = false)
	private String entityName;
	
	@OneToMany(mappedBy = "readAuditLog", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<ReadAuditEntityMetadata> targets = new ArrayList<>();
	
	@Column(name = "read_success")
	private boolean isReadSuccess;
	
	@Column(name = "username", length = 50)
	private String username;
	
	@Column(name = "user_uuid", length = 38, nullable = false)
	private String userUUID;
	
	@Column(name = "event_time", nullable = false)
	private Date eventTime;
	
	@Column(name = "ip_address", length = 50)
	private String ipAddress;
	
	@Column(name = "user_agent", length = 500)
	private String userAgent;
	
	@Column(name = "session_id", length = 100)
	private String sessionId;
	
	@Override
	public void setId(Integer id) {
		if (this.id != null && !this.id.equals(id)) {
			throw new UnsupportedOperationException("Id cannot be mutated once set");
		}
		this.id = id;
	}
	
	public void setTargets(List<ReadAuditEntityMetadata> targets) {
		if (this.targets != null && !this.targets.isEmpty()) {
			throw new UnsupportedOperationException("Targets cannot be mutated once set");
		}
		if (this.targets == null) {
			this.targets = new ArrayList<>();
		}
		if (targets != null) {
			for (ReadAuditEntityMetadata target : targets) {
				target.setReadAuditLog(this);
				this.targets.add(target);
			}
		}
	}
	
	public List<ReadAuditEntityMetadata> getTargets() {
		return targets != null ? Collections.unmodifiableList(targets) : Collections.emptyList();
	}
}
