/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.auditlogweb.web.controller;

import lombok.RequiredArgsConstructor;
import org.openmrs.module.auditlogweb.api.AuditService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;

@Controller
@RequestMapping(value = "module/auditlogweb/viewAudit.form")
@RequiredArgsConstructor
public class ViewAuditController {
    private final AuditService auditService;
    private  final String VIEW_AUDIT = "/module/auditlogweb/viewAudit";

    @RequestMapping(method = RequestMethod.GET)
    public ModelAndView showForm(HttpServletRequest request, ModelMap model) {
        int auditId = Integer.parseInt(request.getParameter("auditId"));
        int entityId = Integer.parseInt(request.getParameter("entityId"));
        Class<?> clazz;
        try {
            clazz = Class.forName(request.getParameter("class"));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        Object currentEntity = auditService.getRevisionById(clazz, entityId, auditId);
        if (auditId - 1 > 0) {
            Object oldEntity = auditService.getRevisionById(clazz, entityId, --auditId);
            model.addAttribute("oldEntity", clazz.cast(oldEntity));
        }
        model.addAttribute("currentEntity", clazz.cast(currentEntity));
        return new ModelAndView(VIEW_AUDIT, model);
    }
}
