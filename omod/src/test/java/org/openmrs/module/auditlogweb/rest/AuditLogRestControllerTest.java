/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.auditlogweb.rest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.hibernate.ObjectNotFoundException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.openmrs.GlobalProperty;
import org.openmrs.Patient;
import org.openmrs.Role;
import org.openmrs.User;
import org.openmrs.api.UserService;
import org.openmrs.api.context.Context;
import org.openmrs.module.auditlogweb.AuditEntity;
import org.openmrs.module.auditlogweb.api.dto.AuditLogDetailDTO;
import org.openmrs.module.auditlogweb.api.utils.UtilClass;
import org.openmrs.module.auditlogweb.api.AuditService;
import org.openmrs.module.auditlogweb.rest.exceptions.RestExceptionHandler;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class AuditLogRestControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private AuditLogRestController auditLogRestController;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(auditLogRestController)
                .setControllerAdvice(new RestExceptionHandler())
                .build();
    }

    @Test
    public void shouldUseEfficientEntityTypeFiltering() throws Exception {
        when(auditService.getAllRevisionsAcrossEntitiesWithEntityType(0, 20, null, null, null, "Patient", "desc"))
                .thenReturn(Collections.emptyList());
        when(auditService.mapAuditEntitiesToDetails(any()))
                .thenReturn(Collections.emptyList());
        when(auditService.countRevisionsAcrossEntitiesWithEntityType(null, null, null, "Patient"))
                .thenReturn(5L);
        mockMvc.perform(get("/rest/v1/auditlogs")
                        .param("entityType", "Patient"))
                .andExpect(status().isOk());

        verify(auditService).getAllRevisionsAcrossEntitiesWithEntityType(0, 20, null, null, null, "Patient", "desc");
        verify(auditService).countRevisionsAcrossEntitiesWithEntityType(null, null, null, "Patient");
    }

    @Test
    public void shouldReturnBadRequestForInvalidDate() throws Exception {
        mockMvc.perform(get("/rest/v1/auditlogs")
                        .param("startDate", "2025/01/01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", is("Invalid date format: '2025/01/01'. Expected format: DD/MM/YYYY")));
    }

    @Test
    public void shouldHandleDateRangeWithoutNPE() throws Exception {
        when(auditService.getAllRevisionsAcrossEntitiesWithEntityType(anyInt(), anyInt(), any(), any(), any(), any(), anyString()))
                .thenReturn(Collections.emptyList());
        when(auditService.mapAuditEntitiesToDetails(any()))
                .thenReturn(Collections.emptyList());
        when(auditService.countRevisionsAcrossEntitiesWithEntityType(any(), any(), any(), any()))
                .thenReturn(0L);

        mockMvc.perform(get("/rest/v1/auditlogs")
                        .param("startDate", "01/01/2023"))
                .andExpect(status().isOk());
    }

    @Test
    public void shouldHandleUnknownEntityTypeGracefully() throws Exception {
        when(auditService.getAllRevisionsAcrossEntitiesWithEntityType(anyInt(), anyInt(), any(), any(), any(), any(), anyString()))
                .thenReturn(Collections.emptyList());
        when(auditService.mapAuditEntitiesToDetails(any()))
                .thenReturn(Collections.emptyList());
        when(auditService.countRevisionsAcrossEntitiesWithEntityType(any(), any(), any(), any()))
                .thenReturn(0L);

        mockMvc.perform(get("/rest/v1/auditlogs")
                        .param("entityType", "UnknownType"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalLogs", is(0)));
    }

    @Test
    public void shouldReturnBadRequestForInvalidPageParameter() throws Exception {
        mockMvc.perform(get("/rest/v1/auditlogs")
                        .param("page", "abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldResolveUsernameToUserId() throws Exception {
        try (MockedStatic<Context> contextMock = mockStatic(Context.class)) {
            UserService userService = mock(UserService.class);
            User mockUser = new User(1);
            contextMock.when(() -> Context.getUserService()).thenReturn(userService);
            when(userService.getUserByUsername("testuser")).thenReturn(mockUser);

            when(auditService.getAllRevisionsAcrossEntitiesWithEntityType(0, 20, 1, null, null, null, "desc"))
                    .thenReturn(Collections.emptyList());
            when(auditService.mapAuditEntitiesToDetails(any()))
                    .thenReturn(Collections.emptyList());
            when(auditService.countRevisionsAcrossEntitiesWithEntityType(1, null, null, null))
                    .thenReturn(1L);

            mockMvc.perform(get("/rest/v1/auditlogs")
                            .param("username", "testuser"))
                    .andExpect(status().isOk());

            verify(auditService).getAllRevisionsAcrossEntitiesWithEntityType(0, 20, 1, null, null, null, "desc");
        }
    }

    @Test
    public void shouldCorrectInvalidPagination() throws Exception {
        when(auditService.getAllRevisionsAcrossEntitiesWithEntityType(0, 20, null, null, null, null, "desc"))
                .thenReturn(Collections.emptyList());
        when(auditService.mapAuditEntitiesToDetails(any()))
                .thenReturn(Collections.emptyList());
        when(auditService.countRevisionsAcrossEntitiesWithEntityType(any(), any(), any(), any()))
                .thenReturn(0L);

        // Negative page and zero size should be corrected to defaults
        mockMvc.perform(get("/rest/v1/auditlogs")
                        .param("page", "-5")
                        .param("size", "0"))
                .andExpect(status().isOk());

        verify(auditService).getAllRevisionsAcrossEntitiesWithEntityType(0, 20, null, null, null, null, "desc");
    }

    @Test
    public void shouldHandleEndDateOnlyWithoutError() throws Exception {
        when(auditService.getAllRevisionsAcrossEntitiesWithEntityType(anyInt(), anyInt(), any(), any(), any(), any(), anyString()))
                .thenReturn(Collections.emptyList());
        when(auditService.mapAuditEntitiesToDetails(any()))
                .thenReturn(Collections.emptyList());
        when(auditService.countRevisionsAcrossEntitiesWithEntityType(any(), any(), any(), any()))
                .thenReturn(0L);

        mockMvc.perform(get("/rest/v1/auditlogs")
                        .param("endDate", "01/01/2024"))
                .andExpect(status().isOk());
    }

    @Test
    public void shouldDefaultTo20WhenSizeIsZeroOrNegative() throws Exception {
        when(auditService.getAllRevisionsAcrossEntitiesWithEntityType(0, 20, null, null, null, null, "desc"))
                .thenReturn(Collections.emptyList());
        when(auditService.mapAuditEntitiesToDetails(any()))
                .thenReturn(Collections.emptyList());
        when(auditService.countRevisionsAcrossEntitiesWithEntityType(any(), any(), any(), any()))
                .thenReturn(0L);

        mockMvc.perform(get("/rest/v1/auditlogs")
                        .param("size", "-10"))
                .andExpect(status().isOk());
    }

    /**
     * Tests {@code AuditLogRestController#getAuditLogByEntity}
     * Verifies the revision endpoint rejects missing required parameters and throws the bad request message.
     */
    @Test
    public void shouldReturnBadRequestWhenFetchEntityRevisionParametersAreMissing() throws Exception {
        mockMvc.perform(get("/rest/v1/auditlogs/fetchEntityRevision")
                        .param("entityName", "")
                        .param("entityId", "")
                        .param("revisionId", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", is("One or more required parameters are empty")));
    }

    /**
     * Tests {@code AuditLogRestController#getAuditLogByEntity}
     * When no request parameters are provided at all, Spring fails binding and
     * the controller advice returns a 500 Internal Server Error.
     */
    @Test
    public void shouldReturnInternalServerErrorWhenFetchEntityRevisionParamsAbsent() throws Exception {
        mockMvc.perform(get("/rest/v1/auditlogs/fetchEntityRevision"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", is("Missing required parameters")));
    }

    /**
     * Tests {@code AuditLogRestController#getAuditLogByEntity}
     * Verifies the revision endpoint resolves an entity class and returns the mapped audit details.
     */
    @Test
    public void shouldFetchEntityRevisionForResolvedEntityClass() throws Exception {
            AuditEntity<?> auditEntity = mock(AuditEntity.class);
            AuditLogDetailDTO auditLogDetailDTO = mock(AuditLogDetailDTO.class);

            try (MockedStatic<UtilClass> utilClassMock = mockStatic(UtilClass.class)) {
                    utilClassMock.when(() -> UtilClass.resolveAuditedEntityClass("Patient")).thenReturn(Patient.class);
                    when(auditService.getAuditEntityRevisionById(Patient.class, 42, 7)).thenReturn((AuditEntity<Patient>) auditEntity);
                    when(auditService.mapAuditEntitiesToDetails(Collections.singletonList(auditEntity)))
                                    .thenReturn(Collections.singletonList(auditLogDetailDTO));

                    mockMvc.perform(get("/rest/v1/auditlogs/fetchEntityRevision")
                                                    .param("entityName", "Patient")
                                                    .param("entityId", "42")
                                                    .param("revisionId", "7"))
                                    .andExpect(status().isOk());

                    verify(auditService).getAuditEntityRevisionById(Patient.class, 42, 7);
                    verify(auditService).mapAuditEntitiesToDetails(Collections.singletonList(auditEntity));
            }
    }

    /**
     * Tests {@code AuditLogRestController#getAuditLogByEntity}
     * Verifies the revision endpoint returns a bad request when the audited record is missing.
     * @throws Exception    throws the bad request error message
     */
    @Test
    public void shouldReturnBadRequestWhenFetchEntityRevisionDataIsMissing() throws Exception {
            try (MockedStatic<UtilClass> utilClassMock = mockStatic(UtilClass.class)) {
                    utilClassMock.when(() -> UtilClass.resolveAuditedEntityClass("Patient")).thenReturn(Patient.class);
                    when(auditService.getAuditEntityRevisionById(Patient.class, 42, 7))
                                    .thenThrow(new ObjectNotFoundException(42,"Patient"));

                    mockMvc.perform(get("/rest/v1/auditlogs/fetchEntityRevision")
                                                    .param("entityName", "Patient")
                                                    .param("entityId", "42")
                                                    .param("revisionId", "7"))
                                    .andExpect(status().isBadRequest());
            }
    }


  
}
