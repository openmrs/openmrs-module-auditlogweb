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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openmrs.Patient;
import org.openmrs.api.PatientService;
import org.openmrs.module.auditlogweb.api.AuditService;
import org.openmrs.module.auditlogweb.rest.exceptions.RestExceptionHandler;
import org.openmrs.module.auditlogweb.api.exception.AuditLogUnavailableException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.Collections;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class PatientLogRestControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuditService auditService;

    @Mock
    private PatientService patientService;

    @InjectMocks
    private PatientLogRestController patientLogRestController;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(patientLogRestController)
                .setControllerAdvice(new RestExceptionHandler())
                .build();
    }

    @Test
    public void shouldReturnBadRequestWhenNoSearchParametersProvided() throws Exception {
        mockMvc.perform(get("/rest/v1/auditlogs/patients"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", is("At least one search parameter must be provided either uuid or id.")));
    }

    @Test
    public void shouldReturnBadRequestWhenPatientNotFound() throws Exception {
        when(patientService.getPatientByUuid("missing-uuid")).thenReturn(null);

        mockMvc.perform(get("/rest/v1/auditlogs/patients").param("uuid", "missing-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", is("No patient found for the given search criteria.")));
    }

    @Test
    public void shouldLookupByUuidAndReturnOk() throws Exception {
        Patient mockPatient = mock(Patient.class);
        when(mockPatient.getPatientId()).thenReturn(1);
        when(patientService.getPatientByUuid("uuid-123")).thenReturn(mockPatient);

        when(auditService.getEntityAuditRevisionsById(1, Patient.class, 0, 20, "desc"))
                .thenReturn(Collections.emptyList());

        when(auditService.getEntityDetailedAudit(any(), eq(Patient.class)))
                .thenReturn(Collections.emptyList());

        when(auditService.countEntityAuditRevisionsById(1, Patient.class))
                .thenReturn(0L);

        mockMvc.perform(get("/rest/v1/auditlogs/patients").param("uuid", "uuid-123"))
                .andExpect(status().isOk());

        verify(auditService).getEntityAuditRevisionsById(1, Patient.class, 0, 20, "desc");
    }

    @Test
    public void shouldLookupByPatientIdAndReturnOk() throws Exception {
        Patient mockPatient = mock(Patient.class);
        when(mockPatient.getPatientId()).thenReturn(3);
        when(patientService.getPatient(3)).thenReturn(mockPatient);

        when(auditService.getEntityAuditRevisionsById(3, Patient.class, 0, 20, "desc"))
                .thenReturn(Collections.emptyList());
        when(auditService.getEntityDetailedAudit(any(), eq(Patient.class)))
                .thenReturn(Collections.emptyList());
        when(auditService.countEntityAuditRevisionsById(3, Patient.class))
                .thenReturn(0L);

        mockMvc.perform(get("/rest/v1/auditlogs/patients").param("id", "3"))
                .andExpect(status().isOk());

        verify(auditService).getEntityAuditRevisionsById(3, Patient.class, 0, 20, "desc");
    }

    @Test
    public void shouldCorrectInvalidPaginationValues() throws Exception {
        Patient mockPatient = mock(Patient.class);
        when(mockPatient.getPatientId()).thenReturn(2);
        when(patientService.getPatientByUuid("uuid-2")).thenReturn(mockPatient);

        when(auditService.getEntityAuditRevisionsById(2, Patient.class, 0, 20, "desc"))
                .thenReturn(Collections.emptyList());
        when(auditService.getEntityDetailedAudit(any(), eq(Patient.class)))
                .thenReturn(Collections.emptyList());
        when(auditService.countEntityAuditRevisionsById(2, Patient.class))
                .thenReturn(0L);

        mockMvc.perform(get("/rest/v1/auditlogs/patients")
                        .param("uuid", "uuid-2")
                        .param("page", "-3")
                        .param("size", "0"))
                .andExpect(status().isOk());

        verify(auditService).getEntityAuditRevisionsById(2, Patient.class, 0, 20, "desc");
    }

    @Test
    public void shouldReturnServiceUnavailableWhenAuditHistoryCannotBeRead() throws Exception {
        Patient mockPatient = mock(Patient.class);
        when(mockPatient.getPatientId()).thenReturn(42);
        when(patientService.getPatientByUuid("uuid-42")).thenReturn(mockPatient);

        when(auditService.getEntityAuditRevisionsById(42, Patient.class, 0, 20, "desc"))
                .thenThrow(new AuditLogUnavailableException("Audit history could not be fetched, try again later", new RuntimeException()));

        mockMvc.perform(get("/rest/v1/auditlogs/patients").param("uuid", "uuid-42"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error", is("Audit Log Unavailable")))
                .andExpect(jsonPath("$.message", is("Audit history could not be fetched, try again later")));

        verify(auditService, never()).countEntityAuditRevisionsById(anyInt(), any());
    }
}
