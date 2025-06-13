/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.auditlogweb.web.controller;

import org.hibernate.QueryException;
import org.openmrs.module.auditlogweb.api.AuditService;
import org.openmrs.module.auditlogweb.api.utils.EnversUtils;
import org.openmrs.module.auditlogweb.web.dto.AuditFieldDiff;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.lang.reflect.Field;

@Controller
@RequestMapping(value = "module/auditlogweb/viewAudit.form")
public class ViewAuditController {

    private final String VIEW = "/module/auditlogweb/viewAudit";
    private final String ENVERS_DISABLED_VIEW = "/module/auditlogweb/enversDisabled";

    private final AuditService auditService;

    public ViewAuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @RequestMapping(method = RequestMethod.GET)
    public ModelAndView showForm(HttpServletRequest request, ModelMap model) {
        if (!EnversUtils.isEnversEnabled()) {
            model.addAttribute("errorMessage", "Audit logging is not enabled on this server.");
            return new ModelAndView(ENVERS_DISABLED_VIEW, model);
        }

        try {
            int auditId = Integer.parseInt(request.getParameter("auditId"));
            int entityId = Integer.parseInt(request.getParameter("entityId"));
            Class<?> clazz = Class.forName(request.getParameter("class"));

            Object currentEntity = auditService.getRevisionById(clazz, entityId, auditId);
            Object oldEntity = (auditId - 1 > 0) ? auditService.getRevisionById(clazz, entityId, auditId - 1) : null;

            List<AuditFieldDiff> diffs = computeFieldDiffs(clazz, oldEntity, currentEntity);
            model.addAttribute("diffs", diffs);
            return new ModelAndView(VIEW, model);

        } catch (IllegalArgumentException e) {
            if (e.getCause() instanceof QueryException) {
                model.addAttribute("errorMessage", "Some referenced data could not be loaded. Please contact your administrator.");
                return new ModelAndView(VIEW, model);
            } else {
                throw e;
            }
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error while loading audit data", e);
        }
    }

    private List<AuditFieldDiff> computeFieldDiffs(Class<?> clazz, Object oldEntity, Object currentEntity) {
        List<AuditFieldDiff> diffs = new ArrayList<>();

        if (currentEntity == null) {
            return diffs;
        }

        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            try {
                Object currentValue = field.get(currentEntity);
                Object oldValue = oldEntity != null ? field.get(oldEntity) : null;

                boolean isDifferent = (oldValue != null && currentValue != null && !oldValue.toString().trim().equals(currentValue.toString().trim()))
                        || (oldValue == null && currentValue != null);

                diffs.add(new AuditFieldDiff(field.getName(),
                        oldValue != null ? oldValue.toString() : "null",
                        currentValue != null ? currentValue.toString() : "null",
                        isDifferent));
            } catch (IllegalAccessException ignored) {
            }
        }

        return diffs;
    }
}