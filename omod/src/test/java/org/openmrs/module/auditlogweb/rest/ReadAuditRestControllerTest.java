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

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.mockito.MockitoAnnotations;
import org.openmrs.module.auditlogweb.ReadAuditLog;
import org.openmrs.module.auditlogweb.api.ReadAuditService;
import org.openmrs.module.auditlogweb.api.dto.ReadAuditLogDTO;
import org.openmrs.module.auditlogweb.rest.exceptions.RestExceptionHandler;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.hamcrest.Matchers.is;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class ReadAuditRestControllerTest {
	
	private MockMvc mockMvc;
	
	@Mock
	private ReadAuditService readAuditService;
	
	@InjectMocks
	private ReadAuditRestController readAuditRestController;
	
	@BeforeEach
	public void setUp() {
		MockitoAnnotations.openMocks(this);
		mockMvc = MockMvcBuilders.standaloneSetup(readAuditRestController).setControllerAdvice(new RestExceptionHandler())
		        .build();
	}
	
	@Test
	public void shouldFetchReadAuditsSuccessfullyWithoutFilter() throws Exception {
		
		when(readAuditService.getReadAuditLogs(null, null, null, null, 0, 15)).thenReturn(Collections.emptyList());
		when(readAuditService.countReadAuditLogs(null, null, null, null)).thenReturn(0L);
		when(readAuditService.mapToReadAuditLogDTO(Collections.emptyList())).thenReturn(Collections.emptyList());
		
		mockMvc.perform(get("/rest/v1/readauditlogs")).andExpect(status().isOk());
		
		verify(readAuditService).getReadAuditLogs(null, null, null, null, 0, 15);
		verify(readAuditService).countReadAuditLogs(null, null, null, null);
		verify(readAuditService).mapToReadAuditLogDTO(Collections.emptyList());
	}
	
	@Test
	public void shouldFetchReadAuditsSuccessfullyWithIdFilter() throws Exception {
		
		ReadAuditLog mockReadAuditLog = mock(ReadAuditLog.class);
		List<ReadAuditLog> logList = Collections.singletonList(mockReadAuditLog);
		when(readAuditService.getReadAuditLogById(1)).thenReturn(mockReadAuditLog);
		Date fixedDate = Date.from(LocalDateTime.of(2026, 12, 25, 14, 30, 0).atZone(ZoneId.of("GMT")).toInstant());
		when(readAuditService.mapToReadAuditLogDTO(logList))
		        .thenReturn(Collections.singletonList(ReadAuditLogDTO.builder().id(1).eventTime(fixedDate).build()));
		
		mockMvc.perform(get("/rest/v1/readauditlogs").param("logId", "1")).andExpect(status().isOk())
		        .andExpect(jsonPath("$.readAuditLogs[0].eventTime", is("25/12/2026 14:30:00")))
		        .andExpect(jsonPath("$.totalLogs", is(1))).andExpect(jsonPath("$.currentLogs", is(1)))
		        .andExpect(jsonPath("$.totalPages", is(1))).andExpect(jsonPath("$.currentPage", is(0)));
		
		verify(readAuditService).getReadAuditLogById(1);
		verify(readAuditService).mapToReadAuditLogDTO(logList);
	}
	
	@Test
	public void shouldThrowErrorIfInvalidLogIddPassed() throws Exception {
		
		mockMvc.perform(get("/rest/v1/readauditlogs").param("logId", "-1")).andExpect(status().isBadRequest())
		        .andExpect(jsonPath("$.error", is("Bad Request")))
		        .andExpect(jsonPath("$.message", is("Please provide a valid log ID")));
	}
	
	@Test
	public void shouldThrowNotFoundErrorIfLogNotFoundForId() throws Exception {
		when(readAuditService.getReadAuditLogById(anyInt())).thenReturn(null);
		mockMvc.perform(get("/rest/v1/readauditlogs").param("logId", "999")).andExpect(status().isNotFound())
		        .andExpect(jsonPath("$.error", is("Not Found")))
		        .andExpect(jsonPath("$.message", is("No log found for this logId")));
	}
	
	@Test
	public void pageSizeShouldNotGoAboveTheCapForFetchReadAuditLogs() throws Exception {
		when(readAuditService.getReadAuditLogs(any(), any(), any(), any(), anyInt(), eq(200)))
		        .thenReturn(Collections.emptyList());
		when(readAuditService.countReadAuditLogs(any(), any(), any(), any())).thenReturn(200L);
		
		mockMvc.perform(get("/rest/v1/readauditlogs").param("size", "10000")).andExpect(status().isOk())
		        .andExpect(jsonPath("$.totalLogs", is(200)));
		
		verify(readAuditService).getReadAuditLogs(any(), any(), any(), any(), eq(0), eq(200));
	}
	
	@Test
	public void shouldFetchReadAuditsSuccessfullyWithEntityNameFilter() throws Exception {
		
		ReadAuditLog mockReadAuditLog = mock(ReadAuditLog.class);
		List<ReadAuditLog> logList = Collections.singletonList(mockReadAuditLog);
		when(readAuditService.getReadAuditLogs("Patient", null, null, null, 0, 15)).thenReturn(logList);
		when(readAuditService.countReadAuditLogs("Patient", null, null, null)).thenReturn(0L);
		when(readAuditService.mapToReadAuditLogDTO(logList)).thenReturn(Collections.emptyList());
		
		mockMvc.perform(get("/rest/v1/readauditlogs").param("entityName", "Patient")).andExpect(status().isOk());
		
		verify(readAuditService).getReadAuditLogs("Patient", null, null, null, 0, 15);
		verify(readAuditService).countReadAuditLogs("Patient", null, null, null);
		verify(readAuditService).mapToReadAuditLogDTO(logList);
	}
	
	@Test
	public void shouldFetchReadAuditsSuccessfullyWithUserNameFilter() throws Exception {
		
		ReadAuditLog mockReadAuditLog = mock(ReadAuditLog.class);
		List<ReadAuditLog> logList = Collections.singletonList(mockReadAuditLog);
		when(readAuditService.getReadAuditLogs(null, "admin", null, null, 0, 15)).thenReturn(logList);
		when(readAuditService.countReadAuditLogs(null, "admin", null, null)).thenReturn(0L);
		when(readAuditService.mapToReadAuditLogDTO(logList)).thenReturn(Collections.emptyList());
		
		mockMvc.perform(get("/rest/v1/readauditlogs").param("username", "admin")).andExpect(status().isOk());
		
		verify(readAuditService).getReadAuditLogs(null, "admin", null, null, 0, 15);
		verify(readAuditService).countReadAuditLogs(null, "admin", null, null);
		verify(readAuditService).mapToReadAuditLogDTO(logList);
	}
	
	@Test
	public void shouldThrowErrorIfInvalidDatePassed() throws Exception {
		
		mockMvc.perform(get("/rest/v1/readauditlogs").param("startDate", "31/02/2025")).andExpect(status().isBadRequest())
		        .andExpect(jsonPath("$.error", is("Bad Request"))).andExpect(jsonPath("$.message",
		            is("Invalid month date or date format: '31/02/2025'. Expected format: DD/MM/YYYY")));
	}
	
	@Test
	public void shouldFetchRelatedAuditsSuccessfully() throws Exception {
		ReadAuditLog mockLog = mock(ReadAuditLog.class);
		List<ReadAuditLog> relatedList = Collections.singletonList(mockLog);
		
		when(readAuditService.getRelatedReadLogs("session-123", 0, 10)).thenReturn(relatedList);
		when(readAuditService.countRelatedReadLogs("session-123")).thenReturn(1L);
		when(readAuditService.mapToReadAuditLogDTO(relatedList)).thenReturn(Collections.emptyList());
		
		mockMvc.perform(get("/rest/v1/readauditlogs/relatedAudits").param("sessionId", "session-123").param("page", "0")
		        .param("size", "10")).andExpect(status().isOk());
		
		verify(readAuditService).getRelatedReadLogs("session-123", 0, 10);
		verify(readAuditService).countRelatedReadLogs("session-123");
		verify(readAuditService).mapToReadAuditLogDTO(relatedList);
	}
	
	@Test
	public void pageSizeShouldNotGoAboveTheCapForFetchRelatedReadAuditLogs() throws Exception {
		when(readAuditService.getRelatedReadLogs(any(), anyInt(), eq(200))).thenReturn(Collections.emptyList());
		when(readAuditService.countRelatedReadLogs(any())).thenReturn(200L);
		
		mockMvc.perform(get("/rest/v1/readauditlogs/relatedAudits").param("sessionId", "session-123").param("size", "10000"))
		        .andExpect(status().isOk()).andExpect(jsonPath("$.totalLogs", is(200)));
		
		verify(readAuditService).getRelatedReadLogs(any(), eq(0), eq(200));
	}
	
}
