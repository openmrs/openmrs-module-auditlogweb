/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.auditlogweb.api;

import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.mockito.ArgumentCaptor;

import org.openmrs.User;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.Daemon;
import org.openmrs.module.auditlogweb.ModuleEvent;
import org.openmrs.module.auditlogweb.ReadAuditLog;
import org.openmrs.module.auditlogweb.api.dao.ModuleEventDao;
import org.openmrs.module.auditlogweb.api.dao.ReadAuditDAO;
import org.openmrs.module.auditlogweb.api.impl.AuditLogRecorderImpl;
import org.openmrs.module.auditlogweb.api.utils.ModuleEventType;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.any;

public class AuditLogRecorderTest {
	
	@Mock
	private ReadAuditDAO readAuditDAO;
	
	@Mock
	private ModuleEventDao moduleEventDao;
	
	@InjectMocks
	private AuditLogRecorderImpl auditLogRecorder;
	
	private MockedStatic<AuditLogContext> mockedAuditLogContext;
	
	private MockedStatic<Context> mockedContext;
	
	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		mockedAuditLogContext = mockStatic(AuditLogContext.class);
		mockedContext = mockStatic(Context.class);
	}
	
	@AfterEach
	void tearDown() {
		mockedAuditLogContext.close();
		mockedContext.close();
	}
	
	@Test
	void shouldDelegateSaveReadAuditLog() {
		ReadAuditLog mockLog = mock(ReadAuditLog.class);
		auditLogRecorder.logReadAudit(mockLog);
		verify(readAuditDAO).saveReadAuditLog(mockLog);
	}
	
	@Test
	void shouldDelegateSaveReadAuditLogs() {
		ReadAuditLog mockLog1 = mock(ReadAuditLog.class);
		ReadAuditLog mockLog2 = mock(ReadAuditLog.class);
		List<ReadAuditLog> logs = Arrays.asList(mockLog1, mockLog2);
		
		auditLogRecorder.logReadAudits(logs);
		
		verify(readAuditDAO).saveReadAuditLog(mockLog1);
		verify(readAuditDAO).saveReadAuditLog(mockLog2);
	}
	
	@Test
	void shouldDoNothingWhenLogReadAuditsIsNull() {
		auditLogRecorder.logReadAudits(null);
	}
	
	@Test
	void shouldNotAbleToAccessThisObjectOutsideThisModule() {
		mockedContext.when(() -> Context.getService(AuditLogRecorder.class))
		        .thenThrow(new APIException("Service not found"));
		assertThrows(APIException.class, () -> Context.getService(AuditLogRecorder.class));
		
	}
	
	@Test
	void shouldLogModuleEventIfAllValuePresentAndCorrect() {
		
		String longUserAgent = StringUtils.repeat("A", 500);
		AuditLogContext ctx = mock(AuditLogContext.class);
		when(ctx.getLoggedInUsername()).thenReturn("admin");
		when(ctx.getLoggedInUserUUID()).thenReturn("user-uuid-123");
		when(ctx.getIpAddress()).thenReturn("127.0.0.1");
		when(ctx.getUserAgent()).thenReturn(longUserAgent);
		when(ctx.getSessionId()).thenReturn("session-abc");
		
		mockedAuditLogContext.when(AuditLogContext::get).thenReturn(ctx);
		auditLogRecorder.logModuleEvent(ModuleEventType.MODULE_LOAD, "Event1", "event", "1.0.0-Snapshot", true, null);
		
		ArgumentCaptor<ModuleEvent> eventCaptor = ArgumentCaptor.forClass(ModuleEvent.class);
		verify(moduleEventDao).saveModuleEvent(eventCaptor.capture());
		ModuleEvent captured = eventCaptor.getValue();
		
		assertEquals("admin", captured.getUsername());
		assertEquals("user-uuid-123", captured.getUserUUID());
		assertEquals(500, captured.getUserAgent().length());
		assertEquals("Event1", captured.getModuleId());
		assertEquals("event", captured.getModuleName());
		assertEquals("1.0.0-Snapshot", captured.getModuleVersion());
		assertTrue(captured.isEventSuccess());
	}
	
	@Test
	void shouldLogFailedModuleEventWithFailureReason() {
		String longUserAgent = StringUtils.repeat("A", 500);
		AuditLogContext ctx = mock(AuditLogContext.class);
		when(ctx.getLoggedInUsername()).thenReturn("admin");
		when(ctx.getLoggedInUserUUID()).thenReturn("user-uuid-123");
		when(ctx.getIpAddress()).thenReturn("127.0.0.1");
		when(ctx.getUserAgent()).thenReturn(longUserAgent);
		when(ctx.getSessionId()).thenReturn("session-abc");
		
		mockedAuditLogContext.when(AuditLogContext::get).thenReturn(ctx);
		String failureReason = StringUtils.repeat("X", 510);
		auditLogRecorder.logModuleEvent(ModuleEventType.MODULE_LOAD, "Event1", "event", "1.0.0-Snapshot", false,
		    failureReason);
		
		ArgumentCaptor<ModuleEvent> eventCaptor = ArgumentCaptor.forClass(ModuleEvent.class);
		verify(moduleEventDao).saveModuleEvent(eventCaptor.capture());
		ModuleEvent captured = eventCaptor.getValue();
		
		assertEquals("admin", captured.getUsername());
		assertEquals("user-uuid-123", captured.getUserUUID());
		assertEquals(500, captured.getUserAgent().length());
		assertEquals("Event1", captured.getModuleId());
		assertEquals("event", captured.getModuleName());
		assertEquals("1.0.0-Snapshot", captured.getModuleVersion());
		assertFalse(captured.isEventSuccess());
		assertEquals(failureReason.substring(0, 500), captured.getFailureReason());
	}
	
	@Test
	void loadModuleEvent_shouldReturnEarlyIfDaemonUser() {
		
		mockedAuditLogContext.when(AuditLogContext::get).thenReturn(null);
		mockedContext.when(Context::isAuthenticated).thenReturn(true);
		
		User daemonUser = mock(User.class);
		mockedContext.when(Context::getAuthenticatedUser).thenReturn(daemonUser);
		
		try (MockedStatic<Daemon> mockedDaemon = mockStatic(Daemon.class)) {
			mockedDaemon.when(() -> Daemon.isDaemonUser(daemonUser)).thenReturn(true);
			auditLogRecorder.logModuleEvent(ModuleEventType.MODULE_LOAD, "Event1", "event", "1.0.0-Snapshot", true, null);
		}
		
		verify(moduleEventDao, never()).saveModuleEvent(any(ModuleEvent.class));
	}
	
	@Test
	void loadModuleEvent_shouldFallbackToAnonymousUserIfNoUserContext() {
		mockedAuditLogContext.when(AuditLogContext::get).thenReturn(null);
		mockedContext.when(Context::isAuthenticated).thenReturn(false);
		
		auditLogRecorder.logModuleEvent(ModuleEventType.MODULE_LOAD, "Event1", "event", "1.0.0-Snapshot", true, null);
		
		ArgumentCaptor<ModuleEvent> eventCaptor = ArgumentCaptor.forClass(ModuleEvent.class);
		verify(moduleEventDao).saveModuleEvent(eventCaptor.capture());
		ModuleEvent captured = eventCaptor.getValue();
		
		assertEquals("anonymous", captured.getUsername());
		assertEquals("anonymous", captured.getUserUUID());
	}
	
	@Test
	void loadModule_shouldReturnEarlyIfInvalidModuleType() {
		mockedAuditLogContext.when(AuditLogContext::get).thenReturn(null);
		
		auditLogRecorder.logModuleEvent(null, "Event1", "event", "1.0.0-Snapshot", true, null);
		
		verify(moduleEventDao, never()).saveModuleEvent(any(ModuleEvent.class));
	}
	
	@Test
	void shouldDelegateSaveModuleEventAuditLog() {
		ModuleEvent mockLog = mock(ModuleEvent.class);
		auditLogRecorder.logModuleEvent(mockLog);
		verify(moduleEventDao).saveModuleEvent(mockLog);
	}
}
