/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.auditlogweb.web.controller;

import org.junit.jupiter.api.Test;
import org.openmrs.module.auditlogweb.api.AuditService;
import org.springframework.ui.ModelMap;
import org.springframework.web.servlet.ModelAndView;
import javax.servlet.http.HttpServletRequest;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ViewAuditControllerTest {

    private final AuditService auditService = mock(AuditService.class);
    private final ViewAuditController controller = new ViewAuditController(auditService);
    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final ModelMap model = new ModelMap();

    @Test
    void shouldReturnCurrentAndOldEntityIfAuditIdGreaterThan1() {
        when(request.getParameter("auditId")).thenReturn("2");
        when(request.getParameter("entityId")).thenReturn("5");
        when(request.getParameter("class")).thenReturn("java.lang.String");

        when(auditService.getRevisionById(String.class, 5, 2)).thenReturn("current");
        when(auditService.getRevisionById(String.class, 5, 1)).thenReturn("old");

        ModelAndView result = controller.showForm(request, model);

        assertEquals("/module/auditlogweb/viewAudit", result.getViewName());
        assertEquals("current", result.getModel().get("currentEntity"));
        assertEquals("old", result.getModel().get("oldEntity"));
    }

    @Test
    void shouldReturnOnlyCurrentEntityIfAuditIdIs1() {
        when(request.getParameter("auditId")).thenReturn("1");
        when(request.getParameter("entityId")).thenReturn("5");
        when(request.getParameter("class")).thenReturn("java.lang.String");

        when(auditService.getRevisionById(String.class, 5, 1)).thenReturn("current");

        ModelAndView result = controller.showForm(request, model);

        assertEquals("current", result.getModel().get("currentEntity"));
        assertNull(result.getModel().get("oldEntity"));
    }
}