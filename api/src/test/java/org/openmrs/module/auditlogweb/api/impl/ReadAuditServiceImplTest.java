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

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openmrs.OpenmrsObject;
import org.openmrs.module.auditlogweb.AppCacheManager;
import org.openmrs.module.auditlogweb.ReadAuditEntityMetadata;
import org.openmrs.module.auditlogweb.ReadAuditLog;
import org.openmrs.module.auditlogweb.ReadAuditWorker;
import org.openmrs.module.auditlogweb.api.AuditLogContext;
import org.openmrs.module.auditlogweb.api.dao.ReadAuditDAO;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.mockito.MockedStatic;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.Daemon;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.any;

class ReadAuditServiceImplTest {
	
	@Mock
	private ReadAuditDAO readAuditDAO;
	
	@Mock
	private AppCacheManager appCacheManager;
	
	@Mock
	private ReadAuditWorker readAuditWorker;
	
	@InjectMocks
	private ReadAuditServiceImpl readAuditService;
	
	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		org.springframework.test.util.ReflectionTestUtils.setField(readAuditService, "appCacheManager", appCacheManager);
		org.springframework.test.util.ReflectionTestUtils.setField(readAuditService, "readAuditWorker", readAuditWorker);
		when(appCacheManager.get(any())).thenReturn(null);
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
	void shouldReturnEmptyListWhenLogNotFound() {
		Date startDate = new Date();
		Date endDate = new Date();
		List<ReadAuditLog> list = Collections.emptyList();
		
		when(readAuditDAO.getReadAuditLogs("Patient", "admin", startDate, endDate, 0, 10)).thenReturn(list);
		
		List<ReadAuditLog> result = readAuditService.getReadAuditLogs("Patient", "admin", startDate, endDate, 0, 10);
		
		assertSame(list, result);
		assertTrue(result.isEmpty());
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
	
	@Test
	void shouldDelegateGetReadAuditLogById() {
		ReadAuditLog mockLog = mock(ReadAuditLog.class);
		when(readAuditDAO.getReadAuditLogById(1)).thenReturn(mockLog);
		
		ReadAuditLog result = readAuditService.getReadAuditLogById(1);
		assertSame(mockLog, result);
		verify(readAuditDAO).getReadAuditLogById(1);
	}
	
	@Test
	void shouldReturnNullWhenLogNotFound() {
		when(readAuditDAO.getReadAuditLogById(1)).thenReturn(null);
		ReadAuditLog result = readAuditService.getReadAuditLogById(1);
		assertNull(result);
	}
	
	@Test
	void shouldDelegateGetRelatedReadAuditLogs() {
		
		List<ReadAuditLog> list = Collections.singletonList(mock(ReadAuditLog.class));
		when(readAuditDAO.getRelatedReadLogs("session-123", 1)).thenReturn(list);
		
		List<ReadAuditLog> result = readAuditService.getRelatedReadLogs("session-123", 1);
		assertSame(list, result);
		verify(readAuditDAO).getRelatedReadLogs("session-123", 1);
	}
	
	@Test
	void shouldReturnEmptyListWhenRelatedReadAuditNotFound() {
		List<ReadAuditLog> list = Collections.emptyList();
		when(readAuditDAO.getRelatedReadLogs("session-123", 1)).thenReturn(list);
		
		List<ReadAuditLog> result = readAuditService.getRelatedReadLogs("session-123", 1);
		
		assertSame(list, result);
		assertTrue(result.isEmpty());
	}
	
	@Test
	void shouldProceedAndLogOnAuditReadRequestSuccess() throws Throwable {
		ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
		MethodSignature signature = mock(MethodSignature.class);
		Method method = TestService.class.getMethod("getSomeData");
		
		when(joinPoint.getTarget()).thenReturn(new Object());
		when(joinPoint.getSignature()).thenReturn(signature);
		when(signature.getMethod()).thenReturn(method);
		when(joinPoint.proceed()).thenReturn("some result");
		
		Object result = readAuditService.auditReadRequest(joinPoint);
		
		assertEquals("some result", result);
		verify(joinPoint).proceed();
	}
	
	@Test
	void shouldProceedAndThrowOnAuditReadRequestFailure() throws Throwable {
		ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
		MethodSignature signature = mock(MethodSignature.class);
		Method method = TestService.class.getMethod("getSomeData");
		RuntimeException expectedException = new RuntimeException("test exception");
		
		when(joinPoint.getTarget()).thenReturn(new Object());
		when(joinPoint.getSignature()).thenReturn(signature);
		when(signature.getMethod()).thenReturn(method);
		when(joinPoint.proceed()).thenThrow(expectedException);
		
		RuntimeException thrown = assertThrows(RuntimeException.class, () -> readAuditService.auditReadRequest(joinPoint));
		
		assertEquals(expectedException, thrown);
		verify(joinPoint).proceed();
	}
	
	@Test
	void shouldSaveReadAuditRequestWithFallbackUserWhenContextIsNull() {
		try (MockedStatic<Context> contextMock = mockStatic(Context.class);
		        MockedStatic<Daemon> daemonMock = mockStatic(Daemon.class)) {
			User mockUser = mock(User.class);
			when(mockUser.getUsername()).thenReturn("fallback-user");
			when(mockUser.getUuid()).thenReturn("fallback-uuid");
			
			contextMock.when(Context::isAuthenticated).thenReturn(true);
			contextMock.when(Context::getAuthenticatedUser).thenReturn(mockUser);
			daemonMock.when(() -> Daemon.isDaemonUser(mockUser)).thenReturn(false);
			
			org.openmrs.OpenmrsObject mockObject = mock(org.openmrs.OpenmrsObject.class);
			when(mockObject.getId()).thenReturn(1);
			when(mockObject.getUuid()).thenReturn("entity-uuid");
			
			readAuditService.saveReadAuditRequest("Patient", true, mockObject);
			
			verify(readAuditWorker).submitTask(any(ReadAuditLog.class));
		}
	}
	
	@Test
	void shouldSkipSaveReadAuditRequestWhenUserIsDaemon() {
		try (MockedStatic<Context> contextMock = mockStatic(Context.class);
		        MockedStatic<Daemon> daemonMock = mockStatic(Daemon.class)) {
			User mockUser = mock(User.class);
			
			contextMock.when(Context::isAuthenticated).thenReturn(true);
			contextMock.when(Context::getAuthenticatedUser).thenReturn(mockUser);
			daemonMock.when(() -> Daemon.isDaemonUser(mockUser)).thenReturn(true);
			
			OpenmrsObject mockObject = mock(OpenmrsObject.class);
			when(mockObject.getId()).thenReturn(1);
			when(mockObject.getUuid()).thenReturn("entity-uuid");
			
			readAuditService.saveReadAuditRequest("Patient", true, mockObject);
			
			verify(readAuditWorker, never()).submitTask(any(ReadAuditLog.class));
		}
	}
	
	@Test
	void shouldSkipSaveReadAuditRequestWhenUserUUIDIsNull() {
		try (MockedStatic<Context> contextMock = mockStatic(Context.class)) {
			contextMock.when(Context::isAuthenticated).thenReturn(false);
			
			OpenmrsObject mockObject = mock(OpenmrsObject.class);
			when(mockObject.getId()).thenReturn(1);
			when(mockObject.getUuid()).thenReturn("entity-uuid");
			
			readAuditService.saveReadAuditRequest("Patient", true, mockObject);
			
			verify(readAuditWorker, never()).submitTask(any(ReadAuditLog.class));
		}
	}
	
	@Test
	void shouldSaveReadAuditLogWhenItsNewInCache() {
		AuditLogContext auditContext = new AuditLogContext();
		auditContext.setLoggedInUsername("test-user");
		auditContext.setLoggedInUserUUID("test-user-uuid");
		auditContext.setIpAddress("127.0.0.1");
		auditContext.setUserAgent("user-agent");
		auditContext.setSessionId("session-id");
		
		AuditLogContext.set(auditContext);
		
		try {
			OpenmrsObject mockObject = mock(OpenmrsObject.class);
			when(mockObject.getId()).thenReturn(1);
			when(mockObject.getUuid()).thenReturn("test-uuid");
			
			String key = "test-user:127.0.0.1:test-uuid";
			when(appCacheManager.get(key)).thenReturn(null);
			
			readAuditService.saveReadAuditRequest("Patient", true, mockObject);
			
			verify(appCacheManager).set(key, true);
			verify(readAuditWorker).submitTask(any(ReadAuditLog.class));
		}
		finally {
			AuditLogContext.clear();
		}
	}
	
	@Test
	void shouldNotSaveReadAuditLogWhenItsAlreadyInCache() {
		AuditLogContext auditContext = new AuditLogContext();
		auditContext.setLoggedInUsername("test-user");
		auditContext.setLoggedInUserUUID("test-user-uuid");
		auditContext.setIpAddress("127.0.0.1");
		auditContext.setUserAgent("user-agent");
		auditContext.setSessionId("session-id");
		
		AuditLogContext.set(auditContext);
		
		try {
			OpenmrsObject mockObject = mock(OpenmrsObject.class);
			when(mockObject.getId()).thenReturn(1);
			when(mockObject.getUuid()).thenReturn("test-uuid");
			
			String key = "test-user:127.0.0.1:test-uuid";
			when(appCacheManager.get(key)).thenReturn(true);
			
			readAuditService.saveReadAuditRequest("Patient", true, mockObject);
			
			verify(appCacheManager, never()).set(any(), any());
			verify(readAuditWorker, never()).submitTask(any(ReadAuditLog.class));
		}
		finally {
			AuditLogContext.clear();
		}
	}
	
	interface TestService {
		
		String getSomeData();
		
	}
}
