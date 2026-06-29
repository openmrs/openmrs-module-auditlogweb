/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.auditlogweb.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.hibernate.envers.RevisionType;
import org.openmrs.GlobalProperty;
import org.openmrs.Role;

import java.util.Date;

@Data
@AllArgsConstructor
public class AuditLogDto {
	
	private Object entity;
	
	private RevisionType revisionType;
	
	private String changedBy;
	
	private Date changedOn;
	
	private Object revisionEntity;
	
	private String entityClassSimpleName;
	
	/**
	 * Gets the appropriate identifier for the entity based on its type
	 *
	 * @return String representation of the entity's identifier
	 */
	public String getEntityIdentifier() {
		if (entity == null) {
			return "NA";
		}
		
		if (entity instanceof Role) {
			return ((Role) entity).getRole();
		} else if (entity instanceof GlobalProperty) {
			return ((GlobalProperty) entity).getProperty();
		} else {
			try {
				return entity.getClass().getMethod("getId").invoke(entity).toString();
			}
			catch (Exception e) {
				return "NA";
			}
		}
	}
}
