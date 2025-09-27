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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openmrs.module.auditlogweb.api.AuditService;
import org.openmrs.module.auditlogweb.api.dto.AuditLogDetailDTO;
import org.openmrs.module.auditlogweb.api.dto.AuditLogResponseDto;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {AuditLogRestController.class})
public class AuditLogRestControllerTest {
    private MockMvc mockMvc;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private AuditLogRestController auditLogRestController;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(auditLogRestController).build();
    }

    @Test
    public void shouldReturnAuditLogsSuccessfully() throws Exception {
        AuditLogDetailDTO detail = new AuditLogDetailDTO();
        detail.setRevisionID(7);
        detail.setChanges(Collections.emptyList());

        AuditLogResponseDto responseDto = new AuditLogResponseDto(1, 0, 1, Collections.singletonList(detail));

        // Mocking the service behavior
        when(auditService.getAllRevisionsAcrossEntities(anyInt(), anyInt(), any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        when(auditService.mapAuditEntitiesToDetails(any()))
                .thenReturn(Collections.singletonList(detail));

        when(auditService.getAuditLogsCount(any(), any(), any(), any()))
                .thenReturn(1L);

        // Perform GET request to the controller
        mockMvc.perform(get("/rest/v1/auditlogs")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalLogs", is(1)))
                .andExpect(jsonPath("$.currentPage", is(0)))
                .andExpect(jsonPath("$.totalPages", is(1)))
                .andExpect(jsonPath("$.logs", hasSize(1)));
    }
}
