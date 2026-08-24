/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.auditlogweb.web.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openmrs.api.APIAuthenticationException;
import org.openmrs.module.auditlogweb.api.utils.ModuleEventType;
import org.openmrs.module.auditlogweb.ModuleEvent;
import org.openmrs.module.auditlogweb.api.ModuleEventService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class ModuleEventWebControllerTest {
	
	private MockMvc mockMvc;
	
	@Mock
	private ModuleEventService moduleEventService;
	
	@InjectMocks
	private ModuleEventWebController controller;
	
	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
	}
	
	@Test
	void shouldReturnDefaultView() throws Exception {
		ModuleEvent event1 = mock(ModuleEvent.class);
		ModuleEvent event2 = mock(ModuleEvent.class);
		List<ModuleEvent> mockEvents = Arrays.asList(event1, event2);
		
		when(moduleEventService.getModuleEvents(null, null, null, null, null, null, null, null, 0, 15))
		        .thenReturn(mockEvents);
		when(moduleEventService.countModuleEvents(null, null, null, null, null, null, null, null)).thenReturn(20L);
		
		mockMvc.perform(get("/module/auditlogweb/moduleEvents.form")).andExpect(status().isOk())
		        .andExpect(view().name("/module/auditlogweb/moduleEvents"))
		        .andExpect(model().attribute("events", mockEvents)).andExpect(model().attribute("totalCount", 20L))
		        .andExpect(model().attribute("totalPages", 2)).andExpect(model().attribute("currentPage", 0))
		        .andExpect(model().attribute("pageSize", 15)).andExpect(model().attribute("hasNextPage", true))
		        .andExpect(model().attribute("hasPreviousPage", false))
		        .andExpect(model().attribute("eventTypes", ModuleEventType.values()))
		        .andExpect(model().attribute("page", "moduleventlogs"));
		
		verify(moduleEventService).getModuleEvents(null, null, null, null, null, null, null, null, 0, 15);
		verify(moduleEventService).countModuleEvents(null, null, null, null, null, null, null, null);
	}
	
	@Test
	void shouldReturnAccessDeniedOnAuthenticationFailure() throws Exception {
		when(moduleEventService.getModuleEvents(any(), any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
		        .thenThrow(new APIAuthenticationException("Not authenticated"));
		
		mockMvc.perform(get("/module/auditlogweb/moduleEvents.form")).andExpect(status().isOk())
		        .andExpect(view().name("/module/auditlogweb/accessDenied"));
	}
	
	@Test
	void shouldHandleGenericExceptions() throws Exception {
		when(moduleEventService.getModuleEvents(any(), any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
		        .thenThrow(new RuntimeException("Database error"));
		
		mockMvc.perform(get("/module/auditlogweb/moduleEvents.form")).andExpect(status().isOk())
		        .andExpect(view().name("/module/auditlogweb/moduleEvents"))
		        .andExpect(model().attribute("errorMessage", "An error occurred while loading module audit logs."))
		        .andExpect(model().attribute("events", hasSize(0)))
		        .andExpect(model().attribute("eventTypes", ModuleEventType.values()))
		        .andExpect(model().attribute("page", "moduleventlogs"));
	}
	
	@Test
	void shouldApplyFiltersAndPagination() throws Exception {
		ModuleEvent event = mock(ModuleEvent.class);
		List<ModuleEvent> mockEvents = Arrays.asList(event);
		
		when(moduleEventService.getModuleEvents(any(), any(), any(), any(), any(), any(), any(Date.class), any(Date.class),
		    anyInt(), anyInt())).thenReturn(mockEvents);
		when(
		    moduleEventService.countModuleEvents(any(), any(), any(), any(), any(), any(), any(Date.class), any(Date.class)))
		        .thenReturn(1L);
		
		mockMvc.perform(get("/module/auditlogweb/moduleEvents.form").param("eventType", "STARTED")
		        .param("moduleName", "reporting").param("username", "admin").param("userUUID", "user-uuid")
		        .param("startDate", "2025-01-01").param("endDate", "2025-01-31").param("page", "1").param("size", "5"))
		        .andExpect(status().isOk()).andExpect(view().name("/module/auditlogweb/moduleEvents"))
		        .andExpect(model().attribute("events", mockEvents)).andExpect(model().attribute("totalCount", 1L))
		        .andExpect(model().attribute("totalPages", 1)).andExpect(model().attribute("hasNextPage", false))
		        .andExpect(model().attribute("hasPreviousPage", true)).andExpect(model().attribute("currentPage", 1))
		        .andExpect(model().attribute("pageSize", 5)).andExpect(model().attribute("eventType", "STARTED"))
		        .andExpect(model().attribute("usernameFilter", "admin"))
		        .andExpect(model().attribute("startDate", "2025-01-01"))
		        .andExpect(model().attribute("endDate", "2025-01-31"));
		
		verify(moduleEventService).getModuleEvents(any(), any(), any(), any(), any(), any(), any(Date.class),
		    any(Date.class), anyInt(), anyInt());
		verify(moduleEventService).countModuleEvents(any(), any(), any(), any(), any(), any(), any(Date.class),
		    any(Date.class));
	}
}
