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
import org.openmrs.module.auditlogweb.api.utils.ModuleEventType;
import org.openmrs.module.auditlogweb.ModuleEvent;
import org.openmrs.module.auditlogweb.api.dao.ModuleEventDao;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ModuleEventServiceImplTest {
	
	@Mock
	private ModuleEventDao moduleEventDao;
	
	@InjectMocks
	private ModuleEventServiceImpl moduleEventService;
	
	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
	}
	
	@Test
	void shouldDelegateGetModuleEventsWithAllFilters() {
		Date startDate = new Date();
		Date endDate = new Date();
		List<ModuleEvent> expected = Collections.singletonList(mock(ModuleEvent.class));
		
		when(moduleEventDao.getModuleEvents(ModuleEventType.MODULE_LOAD, "mod-1", "event", "1.0.0", "admin", "user-uuid-1",
		    startDate, endDate, 0, 10)).thenReturn(expected);
		
		List<ModuleEvent> result = moduleEventService.getModuleEvents(" module_load ", "mod-1", "event", "1.0.0", "admin",
		    "user-uuid-1", startDate, endDate, 0, 10);
		
		assertSame(expected, result);
		verify(moduleEventDao).getModuleEvents(ModuleEventType.MODULE_LOAD, "mod-1", "event", "1.0.0", "admin",
		    "user-uuid-1", startDate, endDate, 0, 10);
	}
	
	@Test
	void shouldDelegateGetModuleEventsWithNullEventType() {
		Date startDate = new Date();
		Date endDate = new Date();
		List<ModuleEvent> expected = Collections.singletonList(mock(ModuleEvent.class));
		
		when(moduleEventDao.getModuleEvents(null, "mod-1", "event", "1.0.0", "admin", "user-uuid-1", startDate, endDate, 1,
		    5)).thenReturn(expected);
		
		List<ModuleEvent> result = moduleEventService.getModuleEvents(null, "mod-1", "event", "1.0.0", "admin",
		    "user-uuid-1", startDate, endDate, 1, 5);
		
		assertSame(expected, result);
		verify(moduleEventDao).getModuleEvents(null, "mod-1", "event", "1.0.0", "admin", "user-uuid-1", startDate, endDate,
		    1, 5);
	}
	
	@Test
	void shouldThrowWhenGetModuleEventsWithInvalidEventType() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
		    () -> moduleEventService.getModuleEvents("unknown", null, null, null, null, null, null, null, 0, 10));
		
		assertEquals("Invalid event type: unknown", exception.getMessage());
	}
	
	@Test
	void shouldDelegateCountModuleEventsWithWithAllFilters() {
		Date startDate = new Date();
		Date endDate = new Date();
		
		when(moduleEventDao.countModuleEvents(ModuleEventType.MODULE_LOAD, "mod-1", "event", "1.0.0", "admin", "user-uuid-1",
		    startDate, endDate)).thenReturn(3L);
		
		long result = moduleEventService.countModuleEvents("module_load", "mod-1", "event", "1.0.0", "admin", "user-uuid-1",
		    startDate, endDate);
		
		assertEquals(3L, result);
		verify(moduleEventDao).countModuleEvents(ModuleEventType.MODULE_LOAD, "mod-1", "event", "1.0.0", "admin",
		    "user-uuid-1", startDate, endDate);
	}
	
	@Test
	void shouldDelegateCountModuleEventsWithNullEventType() {
		Date startDate = new Date();
		Date endDate = new Date();
		
		when(moduleEventDao.countModuleEvents(null, "mod-1", "event", "1.0.0", "admin", "user-uuid-1", startDate, endDate))
		        .thenReturn(7L);
		
		long result = moduleEventService.countModuleEvents(null, "mod-1", "event", "1.0.0", "admin", "user-uuid-1",
		    startDate, endDate);
		
		assertEquals(7L, result);
		verify(moduleEventDao).countModuleEvents(null, "mod-1", "event", "1.0.0", "admin", "user-uuid-1", startDate,
		    endDate);
	}
	
	@Test
	void shouldThrowWhenCountModuleEventsWithInvalidEventType() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
		    () -> moduleEventService.countModuleEvents("bad-type", null, null, null, null, null, null, null));
		
		assertEquals("Invalid event type: bad-type", exception.getMessage());
	}
	
	@Test
	void shouldDelegateGetModuleEventById() {
		ModuleEvent expected = mock(ModuleEvent.class);
		when(moduleEventDao.getModuleEventById(1)).thenReturn(expected);
		
		ModuleEvent result = moduleEventService.getModuleEventById(1);
		
		assertSame(expected, result);
		verify(moduleEventDao).getModuleEventById(1);
	}
	
	@Test
	void shouldReturnNullWhenModuleEventByIdNotFound() {
		when(moduleEventDao.getModuleEventById(1)).thenReturn(null);
		
		ModuleEvent result = moduleEventService.getModuleEventById(1);
		
		assertNull(result);
	}
	
	@Test
	void shouldDelegateGetRelatedModuleEvents() {
		List<ModuleEvent> expected = Collections.singletonList(mock(ModuleEvent.class));
		when(moduleEventDao.getRelatedModuleEvents("session-123", 0, 10)).thenReturn(expected);
		
		List<ModuleEvent> result = moduleEventService.getRelatedModuleEvents("session-123", 0, 10);
		
		assertSame(expected, result);
		verify(moduleEventDao).getRelatedModuleEvents("session-123", 0, 10);
	}
	
	@Test
	void shouldReturnEmptyListWhenRelatedModuleEventsNotFound() {
		List<ModuleEvent> expected = Collections.emptyList();
		when(moduleEventDao.getRelatedModuleEvents("session-123", 0, 10)).thenReturn(expected);
		
		List<ModuleEvent> result = moduleEventService.getRelatedModuleEvents("session-123", 0, 10);
		
		assertSame(expected, result);
		assertTrue(result.isEmpty());
	}
	
	@Test
	void shouldDelegateCountRelatedModuleEvents() {
		when(moduleEventDao.countRelatedModuleEvents("session-123")).thenReturn(5L);
		
		long result = moduleEventService.countRelatedModuleEvents("session-123");
		
		assertEquals(5L, result);
		verify(moduleEventDao).countRelatedModuleEvents("session-123");
	}
}
