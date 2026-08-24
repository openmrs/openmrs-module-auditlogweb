/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.auditlogweb.rest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openmrs.module.auditlogweb.ModuleEvent;
import org.openmrs.module.auditlogweb.api.ModuleEventService;
import org.openmrs.module.auditlogweb.rest.exceptions.RestExceptionHandler;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ModuleEventRestControllerTest {
	
	private MockMvc mockMvc;
	
	@Mock
	private ModuleEventService moduleEventService;
	
	@InjectMocks
	private ModuleEventRestController moduleEventRestController;
	
	@BeforeEach
	public void setUp() {
		MockitoAnnotations.openMocks(this);
		mockMvc = MockMvcBuilders.standaloneSetup(moduleEventRestController).setControllerAdvice(new RestExceptionHandler())
		        .build();
	}
	
	@Test
	public void shouldFetchModuleEventsSuccessfullyWithoutFilter() throws Exception {
		
		when(moduleEventService.getModuleEvents(null, null, null, null, null, null, null, null, 0, 15))
		        .thenReturn(Collections.emptyList());
		when(moduleEventService.countModuleEvents(null, null, null, null, null, null, null, null)).thenReturn(0L);
		when(moduleEventService.mapToModuleEventDTO(Collections.emptyList())).thenReturn(Collections.emptyList());
		
		mockMvc.perform(get("/rest/v1/moduleactionlogs")).andExpect(status().isOk());
		
		verify(moduleEventService).getModuleEvents(null, null, null, null, null, null, null, null, 0, 15);
		verify(moduleEventService).countModuleEvents(null, null, null, null, null, null, null, null);
		verify(moduleEventService).mapToModuleEventDTO(Collections.emptyList());
	}
	
	@Test
	public void shouldFetchModuleEventSuccessfullyWithIdFilter() throws Exception {
		
		ModuleEvent mockModuleEventLog = mock(ModuleEvent.class);
		List<ModuleEvent> logList = Collections.singletonList(mockModuleEventLog);
		when(moduleEventService.getModuleEventById(1)).thenReturn(mockModuleEventLog);
		when(moduleEventService.mapToModuleEventDTO(logList)).thenReturn(Collections.emptyList());
		
		mockMvc.perform(get("/rest/v1/moduleactionlogs").param("logId", "1")).andExpect(status().isOk());
		
		verify(moduleEventService).getModuleEventById(1);
		verify(moduleEventService).mapToModuleEventDTO(logList);
	}
	
	@Test
	public void shouldThrowErrorIfInvalidLogIddPassed() throws Exception {
		
		mockMvc.perform(get("/rest/v1/moduleactionlogs").param("logId", "-1")).andExpect(status().isBadRequest())
		        .andExpect(jsonPath("$.error", is("Bad Request")))
		        .andExpect(jsonPath("$.message", is("Please provide a valid log ID")));
	}
	
	@Test
	public void shouldFetchModuleEventsSuccessfullyWithEventTypeFilter() throws Exception {
		
		ModuleEvent mockModuleEventLog = mock(ModuleEvent.class);
		List<ModuleEvent> logList = Collections.singletonList(mockModuleEventLog);
		when(moduleEventService.getModuleEvents("MODULE_LOAD", null, null, null, null, null, null, null, 0, 15))
		        .thenReturn(logList);
		when(moduleEventService.countModuleEvents("MODULE_LOAD", null, null, null, null, null, null, null)).thenReturn(0L);
		when(moduleEventService.mapToModuleEventDTO(logList)).thenReturn(Collections.emptyList());
		
		mockMvc.perform(get("/rest/v1/moduleactionlogs").param("eventType", "MODULE_LOAD")).andExpect(status().isOk());
		
		verify(moduleEventService).getModuleEvents("MODULE_LOAD", null, null, null, null, null, null, null, 0, 15);
		verify(moduleEventService).countModuleEvents("MODULE_LOAD", null, null, null, null, null, null, null);
		verify(moduleEventService).mapToModuleEventDTO(logList);
	}
	
	@Test
	public void shouldFailFetchModuleEventsWithInvalidEventTypeFilter() throws Exception {
		
		when(moduleEventService.getModuleEvents("load", null, null, null, null, null, null, null, 0, 15))
		        .thenThrow(new IllegalArgumentException("Invalid event type: load"));
		
		mockMvc.perform(get("/rest/v1/moduleactionlogs").param("eventType", "load")).andExpect(status().isBadRequest())
		        .andExpect(jsonPath("$.error", is("Bad Request")))
		        .andExpect(jsonPath("$.message", is("Invalid event type: load")));
	}
	
	@Test
	public void shouldFetchModuleEventsSuccessfullyWithModuleNameFilter() throws Exception {
		
		ModuleEvent mockModuleEventLog = mock(ModuleEvent.class);
		List<ModuleEvent> logList = Collections.singletonList(mockModuleEventLog);
		when(moduleEventService.getModuleEvents(null, null, "Event", null, null, null, null, null, 0, 15))
		        .thenReturn(logList);
		when(moduleEventService.countModuleEvents(null, null, "Event", null, null, null, null, null)).thenReturn(0L);
		when(moduleEventService.mapToModuleEventDTO(logList)).thenReturn(Collections.emptyList());
		
		mockMvc.perform(get("/rest/v1/moduleactionlogs").param("moduleName", "Event")).andExpect(status().isOk());
		
		verify(moduleEventService).getModuleEvents(null, null, "Event", null, null, null, null, null, 0, 15);
		verify(moduleEventService).countModuleEvents(null, null, "Event", null, null, null, null, null);
		verify(moduleEventService).mapToModuleEventDTO(logList);
	}
	
	@Test
	public void shouldFetchModuleEventsSuccessfullyWithUserNameFilter() throws Exception {
		
		ModuleEvent mockModuleEventLog = mock(ModuleEvent.class);
		List<ModuleEvent> logList = Collections.singletonList(mockModuleEventLog);
		when(moduleEventService.getModuleEvents(null, null, null, null, "admin", null, null, null, 0, 15))
		        .thenReturn(logList);
		when(moduleEventService.countModuleEvents(null, null, null, null, "admin", null, null, null)).thenReturn(0L);
		when(moduleEventService.mapToModuleEventDTO(logList)).thenReturn(Collections.emptyList());
		
		mockMvc.perform(get("/rest/v1/moduleactionlogs").param("username", "admin")).andExpect(status().isOk());
		
		verify(moduleEventService).getModuleEvents(null, null, null, null, "admin", null, null, null, 0, 15);
		verify(moduleEventService).countModuleEvents(null, null, null, null, "admin", null, null, null);
		verify(moduleEventService).mapToModuleEventDTO(logList);
	}
	
	@Test
	public void shouldThrowErrorIfInvalidDatePassed() throws Exception {
		
		mockMvc.perform(get("/rest/v1/moduleactionlogs").param("startDate", "31/06/2026")).andExpect(status().isBadRequest())
		        .andExpect(jsonPath("$.error", is("Bad Request"))).andExpect(jsonPath("$.message",
		            is("Invalid month date or date format: '31/06/2026'. Expected format: DD/MM/YYYY")));
	}
	
	@Test
	public void shouldFetchRelatedAuditsSuccessfully() throws Exception {
		ModuleEvent mockLog = mock(ModuleEvent.class);
		List<ModuleEvent> relatedList = Collections.singletonList(mockLog);
		
		when(moduleEventService.getRelatedModuleEvents("session-123", 0, 10)).thenReturn(relatedList);
		when(moduleEventService.countRelatedModuleEvents("session-123")).thenReturn(1L);
		when(moduleEventService.mapToModuleEventDTO(relatedList)).thenReturn(Collections.emptyList());
		
		mockMvc.perform(get("/rest/v1/moduleactionlogs/releatedAudits").param("sessionId", "session-123").param("page", "0")
		        .param("size", "10")).andExpect(status().isOk());
		
		verify(moduleEventService).getRelatedModuleEvents("session-123", 0, 10);
		verify(moduleEventService).countRelatedModuleEvents("session-123");
		verify(moduleEventService).mapToModuleEventDTO(relatedList);
	}
	
}
