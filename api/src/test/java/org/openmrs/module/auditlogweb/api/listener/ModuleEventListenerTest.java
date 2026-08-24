/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.auditlogweb.api.listener;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.openmrs.api.context.Daemon;
import org.openmrs.module.ModuleLoadEvent;
import org.openmrs.module.ModuleStartEvent;
import org.openmrs.module.ModuleStopEvent;
import org.openmrs.module.ModuleUnloadEvent;
import org.openmrs.module.ModuleFactory;
import org.openmrs.module.AbstractModuleEvent;
import org.openmrs.module.auditlogweb.api.utils.ModuleEventType;
import org.openmrs.module.auditlogweb.AuditlogwebActivator;
import org.openmrs.module.auditlogweb.api.AuditLogRecorder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class ModuleEventListenerTest {
	
	@Mock
	private AuditLogRecorder auditLogRecorder;
	
	@InjectMocks
	private ModuleEventListener moduleEventListener;
	
	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
	}
	
	@Test
	void shouldIgnoreModuleActionEventIfDoneByDaemonThread() {
		try (MockedStatic<Daemon> mockStatic = Mockito.mockStatic(Daemon.class)) {
			mockStatic.when(Daemon::isDaemonThread).thenReturn(true);
			
			ModuleLoadEvent mockEvent = (ModuleLoadEvent) buildModuleActionEvent(ModuleEventType.MODULE_LOAD);
			moduleEventListener.listenModuleAction(mockEvent);
			
			verify(auditLogRecorder, never()).logModuleEvent(any(), anyString(), anyString(), anyString(), anyBoolean(),
			    any());
		}
	}
	
	@Test
	void shouldIgnoreModuleActionEventIfOccurDuringShutdown() {
		try (MockedStatic<AuditlogwebActivator> mockStatic = Mockito.mockStatic(AuditlogwebActivator.class)) {
			mockStatic.when(AuditlogwebActivator::isAppShuttingDown).thenReturn(true);
			
			ModuleLoadEvent mockEvent = (ModuleLoadEvent) buildModuleActionEvent(ModuleEventType.MODULE_LOAD);
			moduleEventListener.listenModuleAction(mockEvent);
			
			verify(auditLogRecorder, never()).logModuleEvent(any(), anyString(), anyString(), anyString(), anyBoolean(),
			    any());
		}
	}
	
	@Test
	void shouldListenLoadModuleActionEventSuccessfully() {
		ModuleLoadEvent mockEvent = (ModuleLoadEvent) buildModuleActionEvent(ModuleEventType.MODULE_LOAD);
		
		moduleEventListener.listenModuleAction(mockEvent);
		
		verify(auditLogRecorder).logModuleEvent(ModuleEventType.MODULE_LOAD, "Event1", "Event 1", "1.0.0-Snapshot", true,
		    null);
	}
	
	@Test
	void shouldListenStartModuleActionEventSuccessfully() {
		ModuleStartEvent mockEvent = (ModuleStartEvent) buildModuleActionEvent(ModuleEventType.MODULE_START);
		
		moduleEventListener.listenModuleAction(mockEvent);
		
		verify(auditLogRecorder).logModuleEvent(ModuleEventType.MODULE_START, "Event1", "Event 1", "1.0.0-Snapshot", true,
		    null);
	}
	
	@Test
	void shouldListenStopModuleActionEventSuccessfully() {
		ModuleStopEvent mockEvent = (ModuleStopEvent) buildModuleActionEvent(ModuleEventType.MODULE_STOP);
		
		moduleEventListener.listenModuleAction(mockEvent);
		
		verify(auditLogRecorder).logModuleEvent(ModuleEventType.MODULE_STOP, "Event1", "Event 1", "1.0.0-Snapshot", true,
		    null);
	}
	
	@Test
	void shouldListenUnloadModuleActionEventSuccessfully() {
		ModuleUnloadEvent mockEvent = (ModuleUnloadEvent) buildModuleActionEvent(ModuleEventType.MODULE_UNLOAD);
		
		moduleEventListener.listenModuleAction(mockEvent);
		
		verify(auditLogRecorder).logModuleEvent(ModuleEventType.MODULE_UNLOAD, "Event1", "Event 1", "1.0.0-Snapshot", true,
		    null);
	}
	
	private AbstractModuleEvent buildModuleActionEvent(ModuleEventType moduleEventType) {
		if (moduleEventType == ModuleEventType.MODULE_LOAD) {
			return new ModuleLoadEvent(ModuleFactory.class, "Event1", "Event 1", "1.0.0-Snapshot", true, null);
		} else if (moduleEventType == ModuleEventType.MODULE_START) {
			return new ModuleStartEvent(ModuleFactory.class, "Event1", "Event 1", "1.0.0-Snapshot", true, null);
		} else if (moduleEventType == ModuleEventType.MODULE_STOP) {
			return new ModuleStopEvent(ModuleFactory.class, "Event1", "Event 1", "1.0.0-Snapshot", true, null);
		}
		return new ModuleUnloadEvent(ModuleFactory.class, "Event1", "Event 1", "1.0.0-Snapshot", true, null);
	}
}
