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

class ReadAuditDetailWebControllerTest {
	
	private MockMvc mockMvc;
	
	@Mock
	private ReadAuditService readAuditService;
	
	@InjectMocks
	private ReadAuditDetailWebController controller;
	
	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
	}
	
	@Test
	void shouldReturnErrorMessageWhenNoLogIdProvided() throws Exception {
		mockMvc.perform(get("/module/auditlogweb/viewReadAudit.form")).andExpect(status().isOk())
		        .andExpect(view().name("/module/auditlogweb/viewReadAuditLog"))
		        .andExpect(model().attribute("errorMessage", "No read audit ID provided."));
		
		verifyNoInteractions(readAuditService);
	}
	
	@Test
	void shouldReturnErrorMessageWhenLogIdIsInvalidFormat() throws Exception {
		mockMvc.perform(get("/module/auditlogweb/viewReadAudit.form").param("logId", "abc")).andExpect(status().isOk())
		        .andExpect(view().name("/module/auditlogweb/viewReadAuditLog"))
		        .andExpect(model().attribute("errorMessage", "Invalid readAudit ID format."));
		
		verifyNoInteractions(readAuditService);
	}
	
	@Test
	void shouldReturnErrorMessageWhenReadAuditNotFound() throws Exception {
		when(readAuditService.getReadAuditLogById(99)).thenReturn(null);
		
		mockMvc.perform(get("/module/auditlogweb/viewReadAudit.form").param("logId", "99")).andExpect(status().isOk())
		        .andExpect(view().name("/module/auditlogweb/viewReadAuditLog"))
		        .andExpect(model().attribute("errorMessage", "Read audit not found."));
		
		verify(readAuditService).getReadAuditLogById(99);
	}
	
	@Test
	void shouldLoadReadAuditDetailsWithoutRelatedAudits() throws Exception {
		ReadAuditLog mockLog = mock(ReadAuditLog.class);
		when(mockLog.getSessionId()).thenReturn(null);
		when(readAuditService.getReadAuditLogById(1)).thenReturn(mockLog);
		
		mockMvc.perform(get("/module/auditlogweb/viewReadAudit.form").param("logId", "1")).andExpect(status().isOk())
		        .andExpect(view().name("/module/auditlogweb/viewReadAuditLog"))
		        .andExpect(model().attribute("readAudit", mockLog))
		        .andExpect(model().attribute("relatedAudits", hasSize(0)));
		
		verify(readAuditService).getReadAuditLogById(1);
	}
	
	@Test
	void shouldLoadReadAuditDetailsWithSessionAndRelatedAudits() throws Exception {
		ReadAuditLog mockLog = mock(ReadAuditLog.class);
		ReadAuditLog relatedLog = mock(ReadAuditLog.class);
		List<ReadAuditLog> relatedList = Collections.singletonList(relatedLog);
		
		when(mockLog.getSessionId()).thenReturn("session-test");
		when(readAuditService.getReadAuditLogById(2)).thenReturn(mockLog);
		when(readAuditService.getRelatedReadLogs("session-test", 10)).thenReturn(relatedList);
		
		mockMvc.perform(get("/module/auditlogweb/viewReadAudit.form").param("logId", "2")).andExpect(status().isOk())
		        .andExpect(view().name("/module/auditlogweb/viewReadAuditLog"))
		        .andExpect(model().attribute("readAudit", mockLog))
		        .andExpect(model().attribute("relatedAudits", relatedList));
		
		verify(readAuditService).getReadAuditLogById(2);
		verify(readAuditService).getRelatedReadLogs("session-test", 10);
	}
	
	@Test
	void shouldReturnAccessDeniedOnAuthenticationFailure() throws Exception {
		when(readAuditService.getReadAuditLogById(anyInt())).thenThrow(new APIAuthenticationException("Not authenticated"));
		
		mockMvc.perform(get("/module/auditlogweb/viewReadAudit.form").param("logId", "3")).andExpect(status().isOk())
		        .andExpect(view().name("/module/auditlogweb/accessDenied"));
	}
	
	@Test
	void shouldHandleGenericExceptions() throws Exception {
		when(readAuditService.getReadAuditLogById(anyInt())).thenThrow(new RuntimeException("SQL Error"));
		
		mockMvc.perform(get("/module/auditlogweb/viewReadAudit.form").param("logId", "4")).andExpect(status().isOk())
		        .andExpect(view().name("/module/auditlogweb/viewReadAuditLog"))
		        .andExpect(model().attribute("errorMessage", "Error loading audit data: SQL Error"));
	}
}
