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
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.openmrs.api.APIAuthenticationException;
import org.openmrs.api.context.Context;
import org.openmrs.module.auditlogweb.AuditSecurityEvent;
import org.openmrs.module.auditlogweb.api.AuditService;
import org.openmrs.module.auditlogweb.api.utils.AuditSecurityEventType;
import org.openmrs.module.auditlogweb.rest.exceptions.RestExceptionHandler;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class SecurityAuditRestControllerTest {
	
	private MockMvc mockMvc;
	
	@Mock
	private AuditService auditService;
	
	@InjectMocks
	private SecurityAuditRestController securityAuditRestController;
	
	@BeforeEach
	public void setUp() {
		MockitoAnnotations.openMocks(this);
		mockMvc = MockMvcBuilders.standaloneSetup(securityAuditRestController)
		        .setControllerAdvice(new RestExceptionHandler()).build();
	}
	
	@Test
	public void shouldFetchSecurityAuditsSuccessfullyWithoutFilter() throws Exception {
		when(auditService.getSecurityEvents(null, null, null, null, 0, 15)).thenReturn(Collections.emptyList());
		when(auditService.countSecurityEvents(null, null, null, null)).thenReturn(0L);
		
		mockMvc.perform(get("/rest/v1/securityauditlogs")).andExpect(status().isOk());
		
		verify(auditService).getSecurityEvents(null, null, null, null, 0, 15);
		verify(auditService).countSecurityEvents(null, null, null, null);
	}
	
	@Test
	public void shouldFetchSecurityAuditsSuccessfullyWithIdFilter() throws Exception {
		AuditSecurityEvent event = buildAuditSecurityEvent();
		when(auditService.getSecurityEventById(1)).thenReturn(event);
		
		mockMvc.perform(get("/rest/v1/securityauditlogs").param("logId", "1")).andExpect(status().isOk())
		        .andExpect(jsonPath("$.securityAuditLogs[0].id", is(1)))
		        .andExpect(jsonPath("$.securityAuditLogs[0].eventType", is("LOGIN_SUCCESS")))
		        .andExpect(jsonPath("$.securityAuditLogs[0].username", is("admin")))
		        .andExpect(jsonPath("$.securityAuditLogs[0].userUUID", is("user-uuid-1")))
		        .andExpect(jsonPath("$.securityAuditLogs[0].ipAddress", is("127.0.0.1")))
		        .andExpect(jsonPath("$.securityAuditLogs[0].userAgent", is("user-agent-1")))
		        .andExpect(jsonPath("$.securityAuditLogs[0].sessionId", is("session-123")))
		        .andExpect(jsonPath("$.securityAuditLogs[0].eventTime", is("25/12/2026 14:30:00")))
		        .andExpect(jsonPath("$.securityAuditLogs[0].details", is("{}"))).andExpect(jsonPath("$.totalLogs", is(1)))
		        .andExpect(jsonPath("$.totalPages", is(1))).andExpect(jsonPath("$.currentPage", is(0)));
		
		verify(auditService).getSecurityEventById(1);
	}
	
	@Test
	public void shouldThrowErrorIfInvalidLogIdPassed() throws Exception {
		mockMvc.perform(get("/rest/v1/securityauditlogs").param("logId", "-1")).andExpect(status().isBadRequest())
		        .andExpect(jsonPath("$.error", is("Bad Request")))
		        .andExpect(jsonPath("$.message", is("Please provide a valid log ID")));
	}
	
	@Test
	public void shouldThrowNotFoundErrorIfLogNotFoundForId() throws Exception {
		when(auditService.getSecurityEventById(anyInt())).thenReturn(null);
		mockMvc.perform(get("/rest/v1/securityauditlogs").param("logId", "2121")).andExpect(status().isNotFound())
		        .andExpect(jsonPath("$.error", is("Not Found")))
		        .andExpect(jsonPath("$.message", is("No log found for this logId")));
	}
	
	@Test
	public void shouldThrowErrorIfInvalidEventTypePassed() throws Exception {
		mockMvc.perform(get("/rest/v1/securityauditlogs").param("eventType", "LOGIN_SUCESS"))
		        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error", is("Bad Request")))
		        .andExpect(jsonPath("$.message", is("Invalid eventType LOGIN_SUCESS")));
	}
	
	@Test
	public void shouldReturnStatusOkIfEmptyEventTypePassed() throws Exception {
		mockMvc.perform(get("/rest/v1/securityauditlogs").param("eventType", "")).andExpect(status().isOk());
	}
	
	@Test
	public void pageSizeShouldNotGoAboveTheCapForFetchSecurityAuditLogs() throws Exception {
		when(auditService.getSecurityEvents(any(), any(), any(), any(), anyInt(), eq(200)))
		        .thenReturn(Collections.emptyList());
		when(auditService.countSecurityEvents(any(), any(), any(), any())).thenReturn(200L);
		
		mockMvc.perform(get("/rest/v1/securityauditlogs").param("size", "10000")).andExpect(status().isOk())
		        .andExpect(jsonPath("$.totalLogs", is(200)));
		
		verify(auditService).getSecurityEvents(any(), any(), any(), any(), eq(0), eq(200));
	}
	
	@Test
	public void shouldFetchSecurityAuditsSuccessfullyWithEventTypeFilter() throws Exception {
		AuditSecurityEvent event = buildAuditSecurityEvent();
		List<AuditSecurityEvent> eventList = Collections.singletonList(event);
		
		when(auditService.getSecurityEvents("LOGIN_SUCCESS", null, null, null, 0, 15)).thenReturn(eventList);
		when(auditService.countSecurityEvents("LOGIN_SUCCESS", null, null, null)).thenReturn(1L);
		
		mockMvc.perform(get("/rest/v1/securityauditlogs").param("eventType", "LOGIN_SUCCESS")).andExpect(status().isOk())
		        .andExpect(jsonPath("$.securityAuditLogs[0].id", is(1)))
		        .andExpect(jsonPath("$.securityAuditLogs[0].eventType", is("LOGIN_SUCCESS")))
		        .andExpect(jsonPath("$.securityAuditLogs[0].username", is("admin")))
		        .andExpect(jsonPath("$.securityAuditLogs[0].userUUID", is("user-uuid-1")))
		        .andExpect(jsonPath("$.securityAuditLogs[0].ipAddress", is("127.0.0.1")))
		        .andExpect(jsonPath("$.securityAuditLogs[0].userAgent", is("user-agent-1")))
		        .andExpect(jsonPath("$.securityAuditLogs[0].sessionId", is("session-123")))
		        .andExpect(jsonPath("$.securityAuditLogs[0].eventTime", is("25/12/2026 14:30:00")))
		        .andExpect(jsonPath("$.securityAuditLogs[0].details", is("{}"))).andExpect(jsonPath("$.totalLogs", is(1)))
		        .andExpect(jsonPath("$.currentLogs", is(1))).andExpect(jsonPath("$.totalPages", is(1)))
		        .andExpect(jsonPath("$.currentPage", is(0)));
		
		verify(auditService).getSecurityEvents("LOGIN_SUCCESS", null, null, null, 0, 15);
		verify(auditService).countSecurityEvents("LOGIN_SUCCESS", null, null, null);
	}
	
	@Test
	public void shouldFetchSecurityAuditsSuccessfullyWithUserNameFilter() throws Exception {
		AuditSecurityEvent event = buildAuditSecurityEvent();
		List<AuditSecurityEvent> eventList = Collections.singletonList(event);
		
		when(auditService.getSecurityEvents(null, "admin", null, null, 0, 15)).thenReturn(eventList);
		when(auditService.countSecurityEvents(null, "admin", null, null)).thenReturn(1L);
		
		mockMvc.perform(get("/rest/v1/securityauditlogs").param("username", "admin")).andExpect(status().isOk())
		        .andExpect(jsonPath("$.securityAuditLogs[0].id", is(1)))
		        .andExpect(jsonPath("$.securityAuditLogs[0].eventType", is("LOGIN_SUCCESS")))
		        .andExpect(jsonPath("$.securityAuditLogs[0].username", is("admin")))
		        .andExpect(jsonPath("$.securityAuditLogs[0].userUUID", is("user-uuid-1")))
		        .andExpect(jsonPath("$.securityAuditLogs[0].ipAddress", is("127.0.0.1")))
		        .andExpect(jsonPath("$.securityAuditLogs[0].userAgent", is("user-agent-1")))
		        .andExpect(jsonPath("$.securityAuditLogs[0].sessionId", is("session-123")))
		        .andExpect(jsonPath("$.securityAuditLogs[0].eventTime", is("25/12/2026 14:30:00")))
		        .andExpect(jsonPath("$.securityAuditLogs[0].details", is("{}"))).andExpect(jsonPath("$.totalLogs", is(1)))
		        .andExpect(jsonPath("$.currentLogs", is(1))).andExpect(jsonPath("$.totalPages", is(1)))
		        .andExpect(jsonPath("$.currentPage", is(0)));
		
		verify(auditService).getSecurityEvents(null, "admin", null, null, 0, 15);
		verify(auditService).countSecurityEvents(null, "admin", null, null);
	}
	
	@Test
	public void shouldThrowErrorIfInvalidDatePassed() throws Exception {
		mockMvc.perform(get("/rest/v1/securityauditlogs").param("startDate", "31/02/2025"))
		        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error", is("Bad Request"))).andExpect(jsonPath(
		            "$.message", is("Invalid month date or date format: '31/02/2025'. Expected format: DD/MM/YYYY")));
	}
	
	@Test
	public void shouldFetchRelatedAuditsSuccessfully() throws Exception {
		AuditSecurityEvent event = buildAuditSecurityEvent();
		List<AuditSecurityEvent> relatedList = Collections.singletonList(event);
		
		when(auditService.getRelatedSecurityEvents("session-123", 0, 10)).thenReturn(relatedList);
		when(auditService.countRelatedSecurityEvents("session-123")).thenReturn(1L);
		
		mockMvc.perform(get("/rest/v1/securityauditlogs/relatedAudits").param("sessionId", "session-123").param("page", "0")
		        .param("size", "10")).andExpect(status().isOk()).andExpect(jsonPath("$.securityAuditLogs[0].id", is(1)))
		        .andExpect(jsonPath("$.securityAuditLogs[0].eventType", is("LOGIN_SUCCESS")))
		        .andExpect(jsonPath("$.securityAuditLogs[0].username", is("admin")))
		        .andExpect(jsonPath("$.securityAuditLogs[0].userUUID", is("user-uuid-1")))
		        .andExpect(jsonPath("$.securityAuditLogs[0].ipAddress", is("127.0.0.1")))
		        .andExpect(jsonPath("$.securityAuditLogs[0].userAgent", is("user-agent-1")))
		        .andExpect(jsonPath("$.securityAuditLogs[0].sessionId", is("session-123")))
		        .andExpect(jsonPath("$.securityAuditLogs[0].eventTime", is("25/12/2026 14:30:00")))
		        .andExpect(jsonPath("$.securityAuditLogs[0].details", is("{}"))).andExpect(jsonPath("$.totalLogs", is(1)))
		        .andExpect(jsonPath("$.currentLogs", is(1))).andExpect(jsonPath("$.totalPages", is(1)))
		        .andExpect(jsonPath("$.currentPage", is(0)));
		
		verify(auditService).getRelatedSecurityEvents("session-123", 0, 10);
		verify(auditService).countRelatedSecurityEvents("session-123");
	}
	
	@Test
	public void pageSizeShouldNotGoAboveTheCapForFetchRelatedSecurityAuditLogs() throws Exception {
		when(auditService.getRelatedSecurityEvents(any(), anyInt(), eq(200))).thenReturn(Collections.emptyList());
		when(auditService.countRelatedSecurityEvents(any())).thenReturn(200L);
		
		mockMvc.perform(
		    get("/rest/v1/securityauditlogs/relatedAudits").param("sessionId", "session-123").param("size", "10000"))
		        .andExpect(status().isOk()).andExpect(jsonPath("$.totalLogs", is(200)));
		
		verify(auditService).getRelatedSecurityEvents(any(), eq(0), eq(200));
	}
	
	@Test
	public void shouldThrowUnauthorizedErrorIfNotAuthenticated() throws Exception {
		when(auditService.getSecurityEvents(isNull(), isNull(), isNull(), isNull(), anyInt(), anyInt()))
		        .thenThrow(new APIAuthenticationException("Privileges required: View Security Audit Logs"));
		try (MockedStatic<Context> ctx = mockStatic(Context.class)) {
			ctx.when(Context::isAuthenticated).thenReturn(false);
			mockMvc.perform(get("/rest/v1/securityauditlogs")).andExpect(status().isUnauthorized())
			        .andExpect(jsonPath("$.error", is("Unauthorized")));
		}
	}
	
	@Test
	public void shouldThrowForbiddenErrorIfNotHasPrivileged() throws Exception {
		when(auditService.getSecurityEvents(isNull(), isNull(), isNull(), isNull(), anyInt(), anyInt()))
		        .thenThrow(new APIAuthenticationException("Privileges required: View Security Audit Logs"));
		try (MockedStatic<Context> ctx = mockStatic(Context.class)) {
			ctx.when(Context::isAuthenticated).thenReturn(true);
			mockMvc.perform(get("/rest/v1/securityauditlogs")).andExpect(status().isForbidden())
			        .andExpect(jsonPath("$.error", is("Forbidden")));
		}
	}
	
	private AuditSecurityEvent buildAuditSecurityEvent() {
		Date fixedDate = Date.from(LocalDateTime.of(2026, 12, 25, 14, 30, 0).atZone(ZoneId.of("GMT")).toInstant());
		return AuditSecurityEvent.builder().id(1).eventType(AuditSecurityEventType.LOGIN_SUCCESS).username("admin")
		        .userUuid("user-uuid-1").eventTime(fixedDate).ipAddress("127.0.0.1").userAgent("user-agent-1")
		        .sessionId("session-123").details("{}").build();
	}
}
