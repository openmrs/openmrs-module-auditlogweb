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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openmrs.module.auditlogweb.ReadAuditLog;
import org.openmrs.module.auditlogweb.api.dao.ReadAuditDAO;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReadAuditServiceImplTest {
	
	@Mock
	private ReadAuditDAO readAuditDAO;
	
	@InjectMocks
	private ReadAuditServiceImpl readAuditService;
	
	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
	}
	
	@Test
	void shouldDelegateSaveReadAuditLog() {
		ReadAuditLog mockLog = mock(ReadAuditLog.class);
		readAuditService.logReadAudit(mockLog);
		verify(readAuditDAO).saveReadAuditLog(mockLog);
	}
	
	@Test
	void shouldDelegateSaveReadAuditLogs() {
		ReadAuditLog mockLog1 = mock(ReadAuditLog.class);
		ReadAuditLog mockLog2 = mock(ReadAuditLog.class);
		List<ReadAuditLog> logs = Arrays.asList(mockLog1, mockLog2);
		
		readAuditService.logReadAudits(logs);
		
		verify(readAuditDAO).saveReadAuditLog(mockLog1);
		verify(readAuditDAO).saveReadAuditLog(mockLog2);
	}
	
	@Test
	void shouldDoNothingWhenLogReadAuditsIsNull() {
		readAuditService.logReadAudits(null);
	}
	
	@Test
	void shouldDelegateGetReadAuditLogs() {
		Date startDate = new Date();
		Date endDate = new Date();
		ReadAuditLog mockLog = mock(ReadAuditLog.class);
		List<ReadAuditLog> expectedLogs = Collections.singletonList(mockLog);
		
		when(readAuditDAO.getReadAuditLogs("Patient", "admin", startDate, endDate, 0, 10)).thenReturn(expectedLogs);
		
		List<ReadAuditLog> result = readAuditService.getReadAuditLogs("Patient", "admin", startDate, endDate, 0, 10);
		
		assertSame(expectedLogs, result);
		verify(readAuditDAO).getReadAuditLogs("Patient", "admin", startDate, endDate, 0, 10);
	}
	
	@Test
	void shouldDelegateCountReadAuditLogs() {
		Date startDate = new Date();
		Date endDate = new Date();
		
		when(readAuditDAO.countReadAuditLogs("Patient", "admin", startDate, endDate)).thenReturn(15L);
		
		long count = readAuditService.countReadAuditLogs("Patient", "admin", startDate, endDate);
		
		assertEquals(15L, count);
		verify(readAuditDAO).countReadAuditLogs("Patient", "admin", startDate, endDate);
	}
}
