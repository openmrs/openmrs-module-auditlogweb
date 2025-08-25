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
import org.openmrs.module.auditlogweb.api.dto.AuditLogDetailDTO;
import org.openmrs.module.auditlogweb.api.dto.AuditLogResponseDto;

import java.util.Collections;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.anyInt;

class AuditLogRestControllerTest {

    @Test
    void shouldReturnAuditLogsResponseDto() {
        AuditService mockAuditService = mock(AuditService.class);
        AuditLogRestController controller = new AuditLogRestController(mockAuditService);

        AuditLogDetailDTO sampleLog = new AuditLogDetailDTO();
        sampleLog.setRevisionID(3);
        sampleLog.setEntityType("Person");
        sampleLog.setEventType("Record added");
        sampleLog.setChangedBy("Super User");
        sampleLog.setChangedOn(new Date());
        sampleLog.setChanges(Collections.emptyList());

        when(mockAuditService.mapAuditEntitiesToDetails(any()))
                .thenReturn(Collections.singletonList(sampleLog));
        when(mockAuditService.getAllRevisionsAcrossEntities(anyInt(), anyInt(), any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(mockAuditService.getAuditLogsCount(any(), any(), any(), any()))
                .thenReturn(1L);

        AuditLogResponseDto response = controller.getAuditLogs(0, 20, null, null, null, null, null);

        assertNotNull(response);
        assertEquals(1, response.getTotalLogs());
        assertEquals(0, response.getCurrentPage());
        assertEquals(1, response.getTotalPages());
        assertEquals("Record added", response.getLogs().get(0).getEventType());
        assertEquals("Person", response.getLogs().get(0).getEntityType());
        assertEquals("Super User", response.getLogs().get(0).getChangedBy());
    }
}
