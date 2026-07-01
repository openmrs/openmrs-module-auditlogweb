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
import org.openmrs.module.auditlogweb.ReadAuditLog;
import org.openmrs.module.auditlogweb.api.ReadAuditService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;

class ReadAuditWebControllerTest {
	
	private MockMvc mockMvc;
	
	@Mock
	private ReadAuditService readAuditService;
	
	@InjectMocks
	private ReadAuditWebController controller;
	
	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
	}
	
	@Test
	void shouldReturnDefaultView() throws Exception {
		ReadAuditLog event1 = mock(ReadAuditLog.class);
		ReadAuditLog event2 = mock(ReadAuditLog.class);
		List<ReadAuditLog> mockEvents = Arrays.asList(event1, event2);
		
		when(readAuditService.getReadAuditLogs(null, null, null, null, 0, 15)).thenReturn(mockEvents);
		when(readAuditService.countReadAuditLogs(null, null, null, null)).thenReturn(20L);
		
		mockMvc.perform(get("/module/auditlogweb/readauditlogs.form")).andExpect(status().isOk())
		        .andExpect(view().name("/module/auditlogweb/readAuditLogs"))
		        .andExpect(model().attribute("readAuditLogs", mockEvents)).andExpect(model().attribute("totalCount", 20L))
		        .andExpect(model().attribute("totalPages", 2)).andExpect(model().attribute("currentPage", 0))
		        .andExpect(model().attribute("pageSize", 15)).andExpect(model().attribute("hasNextPage", true))
		        .andExpect(model().attribute("hasPreviousPage", false))
		        .andExpect(model().attribute("page", "readauditlogs"));
		
		verify(readAuditService).getReadAuditLogs(null, null, null, null, 0, 15);
		verify(readAuditService).countReadAuditLogs(null, null, null, null);
	}
	
	@Test
	void shouldReturnAccessDeniedOnAuthenticationFailure() throws Exception {
		when(readAuditService.getReadAuditLogs(any(), any(), any(), any(), anyInt(), anyInt()))
		        .thenThrow(new APIAuthenticationException("Not authenticated"));
		
		mockMvc.perform(get("/module/auditlogweb/readauditlogs.form")).andExpect(status().isOk())
		        .andExpect(view().name("/module/auditlogweb/accessDenied"));
	}
	
	@Test
	void shouldHandleGenericExceptions() throws Exception {
		when(readAuditService.getReadAuditLogs(any(), any(), any(), any(), anyInt(), anyInt()))
		        .thenThrow(new RuntimeException("Database error"));
		
		mockMvc.perform(get("/module/auditlogweb/readauditlogs.form")).andExpect(status().isOk())
		        .andExpect(view().name("/module/auditlogweb/readAuditLogs"))
		        .andExpect(model().attribute("errorMessage", "An error occurred while loading security audit logs."))
		        .andExpect(model().attribute("readAuditLogs", hasSize(0)));
	}
}
