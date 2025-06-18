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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger logger = LoggerFactory.getLogger(ViewAuditController.class);
    private final String VIEW = MODULE_PATH + "/viewAudit";
    private final String ENVERS_DISABLED_VIEW = MODULE_PATH + "/enversDisabled";

    private final AuditService auditService;

    @RequestMapping(method = RequestMethod.GET)
    public ModelAndView showForm(HttpServletRequest request, ModelMap model) {
        if (!EnversUtils.isEnversEnabled()) {
            model.addAttribute("errorMessage", EnversUtils.getAdminHint());
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
                } catch (org.hibernate.ObjectNotFoundException ignored) {}
            }
            List<AuditFieldDiff> diffs = UtilClass.computeFieldDiffs(clazz, oldEntity, currentEntity);
            String auditType;
            if (oldEntity == null && currentEntity != null) {
                auditType = "Record was added";
            } else if (oldEntity != null && currentEntity == null) {
                auditType = "Record was deleted";
            } else if (oldEntity != null && currentEntity != null) {
                auditType = "Record was modified";
            } else {
                auditType = "No change";
            }
            model.addAttribute("auditType", auditType);
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
            logger.error("Error loading audit data: ", e);
            return new ModelAndView(VIEW, model);
        }
    }
}