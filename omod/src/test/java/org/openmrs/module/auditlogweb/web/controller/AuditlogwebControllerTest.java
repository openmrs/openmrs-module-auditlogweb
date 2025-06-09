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
import org.openmrs.module.auditlogweb.AuditEntity;
import org.openmrs.module.auditlogweb.api.AuditService;
import org.springframework.ui.Model;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class AuditlogwebControllerTest {

    private AuditService auditService;
    private AuditlogwebController controller;
    private Model model;

    @BeforeEach
    void setup() {
        auditService = mock(AuditService.class);
        model = mock(Model.class);
        controller = new AuditlogwebController(auditService);
    }

    @Test
    void onGet_shouldReturnViewName() {
        assertEquals("/module/auditlogweb/auditlogs", controller.onGet());
    }

    @Test
    @SuppressWarnings("unchecked")
    void showClassFormAndAudits_withClassName_shouldAddModelAttributes() {
        String className = "TestClass";
        AuditEntity<Object> audit1 = mock(AuditEntity.class);
        AuditEntity<Object> audit2 = mock(AuditEntity.class);
        List<AuditEntity<Object>> revisions = Arrays.asList(audit1, audit2);
        when(auditService.getAllRevisions(className)).thenReturn(revisions);

        String view = controller.showClassFormAndAudits(className, model);
        verify(auditService).getAllRevisions(className);
        verify(model).addAttribute("audits", revisions);
        verify(model).addAttribute("currentClass", className);
        assertEquals("/module/auditlogweb/auditlogs", view);
    }

    @Test
    void showClassFormAndAudits_withoutClassName_shouldNotCallService() {
        String view = controller.showClassFormAndAudits(null, model);

        verifyNoInteractions(auditService);
        assertEquals("/module/auditlogweb/auditlogs", view);
    }
}