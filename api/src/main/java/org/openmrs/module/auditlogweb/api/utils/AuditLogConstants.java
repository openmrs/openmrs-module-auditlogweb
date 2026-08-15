/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.auditlogweb.api.utils;

import org.openmrs.PatientIdentifier;
import org.openmrs.PersonAddress;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonName;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class AuditLogConstants {
	
	/* MODULE PRIVILEGES */
	public static final String VIEW_AUDIT_LOGS = "View Audit Logs";
	
	public static final String VIEW_SECURITY_AUDIT_LOGS = "View Security Audit Logs";
	
	public static final String VIEW_READ_AUDIT_LOGS = "View Read Audit Logs";
	
	public static final Map<Class<?>, String> PATIENT_OWNED_AUDITED_TYPES;
	
	private static final String OWNER_PROPERTY_PERSON = "person";
	
	static {
		Map<Class<?>, String> types = new LinkedHashMap<>();
		types.put(PersonName.class, OWNER_PROPERTY_PERSON);
		types.put(PersonAddress.class, OWNER_PROPERTY_PERSON);
		types.put(PersonAttribute.class, OWNER_PROPERTY_PERSON);
		types.put(PatientIdentifier.class, "patient");
		PATIENT_OWNED_AUDITED_TYPES = Collections.unmodifiableMap(types);
	}
	
	private AuditLogConstants() {
	}
}
