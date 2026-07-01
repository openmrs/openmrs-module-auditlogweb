/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.auditlogweb.api.aop;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.openmrs.module.auditlogweb.api.ReadAuditService;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class ReadAuditAOP {
	
	private final ReadAuditService readAuditService;
	
	@Around("execution(* org.openmrs.api.PatientService.getPatient*(..)) || "
	        + "execution(* org.openmrs.api.PatientService.getAllPatient*(..)) || "
	        + "execution(* org.openmrs.api.PatientService.getDuplicatePatient*(..)) || "
	        + "execution(* org.openmrs.api.PatientService.getAllerg*(..)) ")
	public Object auditPatientDataRead(ProceedingJoinPoint joinPoint) throws Throwable {
		return readAuditService.auditReadRequest(joinPoint);
	}
	
	@Around("execution(* org.openmrs.api.EncounterService.getEncounter*(..)) || "
	        + "execution(* org.openmrs.api.EncounterService.getAllEncounter*(..))")
	public Object auditEncounterDataRead(ProceedingJoinPoint joinPoint) throws Throwable {
		return readAuditService.auditReadRequest(joinPoint);
	}
	
}
