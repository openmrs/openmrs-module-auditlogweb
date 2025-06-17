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
import org.hibernate.QueryException;
import org.openmrs.module.auditlogweb.api.AuditService;
import org.openmrs.module.auditlogweb.api.utils.EnversUtils;
import org.openmrs.module.auditlogweb.api.dto.AuditFieldDiff;
import org.openmrs.module.auditlogweb.api.utils.UtilClass;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import static org.openmrs.module.auditlogweb.AuditlogwebConstants.MODULE_PATH;

@Controller("auditlogweb.ViewAuditController")
@RequestMapping(value = MODULE_PATH + "/viewAudit.form")
@RequiredArgsConstructor
public class ViewAuditController {

    private final String VIEW = MODULE_PATH + "/viewAudit";
    private final String ENVERS_DISABLED_VIEW = MODULE_PATH + "/enversDisabled";

    private final AuditService auditService;

    @RequestMapping(method = RequestMethod.GET)
    public ModelAndView showForm(HttpServletRequest request, ModelMap model) {
        if (!EnversUtils.isEnversEnabled()) {
            model.addAttribute("errorMessage", "Audit logging is not enabled on this server.");
            return new ModelAndView(ENVERS_DISABLED_VIEW, model);
        }

        String auditIdParam = request.getParameter("auditId");
        String entityIdParam = request.getParameter("entityId");
        String className = request.getParameter("class");

        if (auditIdParam == null || entityIdParam == null || className == null || auditIdParam.isEmpty() || entityIdParam.isEmpty() || className.isEmpty()) {
            model.addAttribute("errorMessage", "Please select an entity to view audit details.");
            return new ModelAndView(VIEW, model);
        }

        try {
            int auditId = Integer.parseInt(auditIdParam);
            int entityId = Integer.parseInt(entityIdParam);
            Class<?> clazz = Class.forName(className);
            Object currentEntity = null;
            Object oldEntity = null;

            try {
                currentEntity = auditService.getRevisionById(clazz, entityId, auditId);
            } catch (org.hibernate.ObjectNotFoundException ex) {
                model.addAttribute("errorMessage", "Audit entity not found for this revision.");
                return new ModelAndView(VIEW, model);
            }

            if (auditId - 1 > 0) {
                try {
                    oldEntity = auditService.getRevisionById(clazz, entityId, auditId - 1);
                } catch (org.hibernate.ObjectNotFoundException ex) {
                    oldEntity = null;
                }
            }
            List<AuditFieldDiff> diffs = UtilClass.computeFieldDiffs(clazz, oldEntity, currentEntity);
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
            model.addAttribute("errorMessage", "Error loading audit data: " + e.getMessage());
            e.printStackTrace();
            return new ModelAndView(VIEW, model);
        }
    }

//    private List<AuditFieldDiff> computeFieldDiffs(Class<?> clazz, Object oldEntity, Object currentEntity) {
//        List<AuditFieldDiff> diffs = new ArrayList<>();
//
//        if (currentEntity == null) {
//            return diffs;
//        }
//
//        Field[] fields = clazz.getDeclaredFields();
//        for (Field field : fields) {
//            if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
//                continue;
//            }
//
//            field.setAccessible(true);
//            Object currentValue = null;
//            Object oldValue = null;
//
//            try {
//                currentValue = field.get(currentEntity);
//            } catch (IllegalAccessException ignored) {
//            }
//
//            try {
//                if (oldEntity != null) {
//                    oldValue = field.get(oldEntity);
//                }
//            } catch (IllegalAccessException ignored) {
//            }
//
//            String oldStr = safeToString(oldValue);
//            String currStr = safeToString(currentValue);
//
//            boolean isDifferent = !Objects.equals(oldStr, currStr);
//
//            diffs.add(new AuditFieldDiff(field.getName(), oldStr, currStr, isDifferent));
//        }
//
//        return diffs;
//    }
//
//    private String safeToString(Object obj) {
//        if (obj == null) return "null";
//        try {
//            return obj.toString();
//        } catch (org.hibernate.ObjectNotFoundException | org.hibernate.LazyInitializationException e) {
//            return "Data not found";
//        } catch (Exception e) {
//            return "Data not found";
//        }
//    }

}