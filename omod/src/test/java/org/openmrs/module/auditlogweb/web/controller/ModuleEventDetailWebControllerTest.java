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
import org.openmrs.module.auditlogweb.ModuleEvent;
import org.openmrs.module.auditlogweb.api.ModuleEventService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class ModuleEventDetailWebControllerTest {
	
	private MockMvc mockMvc;
	
	@Mock
	private ModuleEventService moduleEventService;
	
	@InjectMocks
	private ModuleEventDetailWebController controller;
	
	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
	}
	
	@Test
	void shouldReturnErrorMessageWhenNoEventIdProvided() throws Exception {
		mockMvc.perform(get("/module/auditlogweb/viewModuleEvent.form")).andExpect(status().isOk())
		        .andExpect(view().name("/module/auditlogweb/viewModuleEvent"))
		        .andExpect(model().attribute("errorMessage", "No event ID provided."));
		
		verifyNoInteractions(moduleEventService);
	}
	
	@Test
	void shouldReturnErrorMessageWhenEventIdIsInvalidFormat() throws Exception {
		mockMvc.perform(get("/module/auditlogweb/viewModuleEvent.form").param("eventId", "abc")).andExpect(status().isOk())
		        .andExpect(view().name("/module/auditlogweb/viewModuleEvent"))
		        .andExpect(model().attribute("errorMessage", "Invalid event ID format."));
		
		verifyNoInteractions(moduleEventService);
	}
	
	@Test
	void shouldReturnErrorMessageWhenModuleEventNotFound() throws Exception {
		when(moduleEventService.getModuleEventById(99)).thenReturn(null);
		
		mockMvc.perform(get("/module/auditlogweb/viewModuleEvent.form").param("eventId", "99")).andExpect(status().isOk())
		        .andExpect(view().name("/module/auditlogweb/viewModuleEvent"))
		        .andExpect(model().attribute("errorMessage", "Module event log not found."));
		
		verify(moduleEventService).getModuleEventById(99);
	}
	
	@Test
	void shouldLoadModuleEventDetailsWithoutRelatedEvents() throws Exception {
		ModuleEvent mockEvent = mock(ModuleEvent.class);
		when(mockEvent.getSessionId()).thenReturn(null);
		when(moduleEventService.getModuleEventById(1)).thenReturn(mockEvent);
		
		mockMvc.perform(get("/module/auditlogweb/viewModuleEvent.form").param("eventId", "1")).andExpect(status().isOk())
		        .andExpect(view().name("/module/auditlogweb/viewModuleEvent"))
		        .andExpect(model().attribute("event", mockEvent)).andExpect(model().attribute("relatedEvents", hasSize(0)));
		
		verify(moduleEventService).getModuleEventById(1);
	}
	
	@Test
	void shouldLoadModuleEventDetailsWithSessionAndRelatedEvents() throws Exception {
		ModuleEvent mockEvent = mock(ModuleEvent.class);
		ModuleEvent relatedEvent = mock(ModuleEvent.class);
		List<ModuleEvent> relatedEvents = Collections.singletonList(relatedEvent);
		
		when(mockEvent.getSessionId()).thenReturn("session-test");
		when(moduleEventService.getModuleEventById(2)).thenReturn(mockEvent);
		when(moduleEventService.getRelatedModuleEvents("session-test", 0, 10)).thenReturn(relatedEvents);
		
		mockMvc.perform(get("/module/auditlogweb/viewModuleEvent.form").param("eventId", "2")).andExpect(status().isOk())
		        .andExpect(view().name("/module/auditlogweb/viewModuleEvent"))
		        .andExpect(model().attribute("event", mockEvent))
		        .andExpect(model().attribute("relatedEvents", relatedEvents));
		
		verify(moduleEventService).getModuleEventById(2);
		verify(moduleEventService).getRelatedModuleEvents("session-test", 0, 10);
	}
	
	@Test
	void shouldReturnAccessDeniedOnAuthenticationFailure() throws Exception {
		when(moduleEventService.getModuleEventById(anyInt())).thenThrow(new APIAuthenticationException("Not authenticated"));
		
		mockMvc.perform(get("/module/auditlogweb/viewModuleEvent.form").param("eventId", "3")).andExpect(status().isOk())
		        .andExpect(view().name("/module/auditlogweb/accessDenied"));
	}
	
	@Test
	void shouldHandleGenericExceptions() throws Exception {
		when(moduleEventService.getModuleEventById(anyInt())).thenThrow(new RuntimeException("SQL Error"));
		
		mockMvc.perform(get("/module/auditlogweb/viewModuleEvent.form").param("eventId", "4")).andExpect(status().isOk())
		        .andExpect(view().name("/module/auditlogweb/viewModuleEvent"))
		        .andExpect(model().attribute("errorMessage", "Error loading audit data: SQL Error"));
	}
}
