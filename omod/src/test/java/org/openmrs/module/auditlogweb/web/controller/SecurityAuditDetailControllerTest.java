/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
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
import org.openmrs.module.auditlogweb.AuditSecurityEvent;
import org.openmrs.module.auditlogweb.api.AuditService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class SecurityAuditDetailControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private SecurityAuditDetailController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void shouldReturnErrorMessageWhenNoEventIdProvided() throws Exception {
        mockMvc.perform(get("/module/auditlogweb/viewSecurityAudit.form"))
                .andExpect(status().isOk())
                .andExpect(view().name("/module/auditlogweb/viewSecurityAudit"))
                .andExpect(model().attribute("errorMessage", "No event ID provided."));

        verifyNoInteractions(auditService);
    }

    @Test
    void shouldReturnErrorMessageWhenEventIdIsInvalidFormat() throws Exception {
        mockMvc.perform(get("/module/auditlogweb/viewSecurityAudit.form").param("eventId", "abc"))
                .andExpect(status().isOk())
                .andExpect(view().name("/module/auditlogweb/viewSecurityAudit"))
                .andExpect(model().attribute("errorMessage", "Invalid event ID format."));

        verifyNoInteractions(auditService);
    }

    @Test
    void shouldReturnErrorMessageWhenEventNotFound() throws Exception {
        when(auditService.getSecurityEventById(99)).thenReturn(null);

        mockMvc.perform(get("/module/auditlogweb/viewSecurityAudit.form").param("eventId", "99"))
                .andExpect(status().isOk())
                .andExpect(view().name("/module/auditlogweb/viewSecurityAudit"))
                .andExpect(model().attribute("errorMessage", "Security event not found."));

        verify(auditService).getSecurityEventById(99);
    }

    @Test
    void shouldLoadEventDetailsWithoutRelatedEvents() throws Exception {
        AuditSecurityEvent mockEvent = mock(AuditSecurityEvent.class);
        when(mockEvent.getSessionId()).thenReturn(null);
        when(auditService.getSecurityEventById(1)).thenReturn(mockEvent);

        mockMvc.perform(get("/module/auditlogweb/viewSecurityAudit.form").param("eventId", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("/module/auditlogweb/viewSecurityAudit"))
                .andExpect(model().attribute("event", mockEvent))
                .andExpect(model().attribute("relatedEvents", hasSize(0)))
                .andExpect(model().attribute("page", "securityauditlogs"));

        verify(auditService).getSecurityEventById(1);
    }

    @Test
    void shouldLoadEventDetailsWithSessionAndRelatedEvents() throws Exception {
        AuditSecurityEvent mockEvent = mock(AuditSecurityEvent.class);
        AuditSecurityEvent relatedEvent = mock(AuditSecurityEvent.class);
        List<AuditSecurityEvent> relatedList = Collections.singletonList(relatedEvent);

        when(mockEvent.getSessionId()).thenReturn("session-test");
        when(auditService.getSecurityEventById(2)).thenReturn(mockEvent);
        when(auditService.getRelatedSecurityEvents("session-test", 10)).thenReturn(relatedList);

        mockMvc.perform(get("/module/auditlogweb/viewSecurityAudit.form").param("eventId", "2"))
                .andExpect(status().isOk())
                .andExpect(view().name("/module/auditlogweb/viewSecurityAudit"))
                .andExpect(model().attribute("event", mockEvent))
                .andExpect(model().attribute("relatedEvents", relatedList))
                .andExpect(model().attribute("page", "securityauditlogs"));

        verify(auditService).getSecurityEventById(2);
        verify(auditService).getRelatedSecurityEvents("session-test", 10);
    }

    @Test
    void shouldReturnAccessDeniedOnAuthenticationFailure() throws Exception {
        when(auditService.getSecurityEventById(anyInt()))
                .thenThrow(new APIAuthenticationException("Not authenticated"));

        mockMvc.perform(get("/module/auditlogweb/viewSecurityAudit.form").param("eventId", "3"))
                .andExpect(status().isOk())
                .andExpect(view().name("/module/auditlogweb/accessDenied"));
    }

    @Test
    void shouldHandleGenericExceptions() throws Exception {
        when(auditService.getSecurityEventById(anyInt()))
                .thenThrow(new RuntimeException("SQL Error"));

        mockMvc.perform(get("/module/auditlogweb/viewSecurityAudit.form").param("eventId", "4"))
                .andExpect(status().isOk())
                .andExpect(view().name("/module/auditlogweb/viewSecurityAudit"))
                .andExpect(model().attribute("errorMessage", "Error loading audit data: SQL Error"));
    }
}
