/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.auditlogweb.rest;

import org.junit.jupiter.api.Test;
import org.openmrs.module.auditlogweb.api.AuditService;
import org.openmrs.module.auditlogweb.api.dto.AuditLogResponseDto;
import org.openmrs.module.auditlogweb.api.dto.RestAuditLogDto;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

class AuditLogRestControllerTest {

    @Test
    void shouldReturnAuditLogsResponseDto() {
        AuditService mockAuditService = mock(AuditService.class);
        AuditLogRestController controller = new AuditLogRestController(mockAuditService);

        RestAuditLogDto sampleLog = new RestAuditLogDto();
        sampleLog.setEventType("ADD");
        sampleLog.setEntityId("3");
        sampleLog.setChangedBy("Super User");

        when(mockAuditService.getAllAuditLogs(anyInt(), anyInt(), any(), any(), any(), any()))
                .thenReturn(Collections.singletonList(sampleLog));
        when(mockAuditService.getAuditLogsCount(any(), any(), any(), any()))
                .thenReturn(1L);

        AuditLogResponseDto response = controller.getAllAuditLogs(0, 20, null, null, null, null, null);

        assertNotNull(response);
        assertEquals(1, response.getTotalLogs());
        assertEquals(0, response.getCurrentPage());
        assertEquals(1, response.getTotalPages()); // totalPages = ceil(1 / 20) = 1
        assertEquals("ADD", response.getLogs().get(0).getEventType());
        assertEquals("3", response.getLogs().get(0).getEntityId());
        assertEquals("Super User", response.getLogs().get(0).getChangedBy());
    }
}
