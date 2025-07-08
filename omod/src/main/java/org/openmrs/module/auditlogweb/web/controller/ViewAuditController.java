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
import org.openmrs.module.auditlogweb.AuditEntity;
import org.openmrs.module.auditlogweb.api.AuditService;
import org.openmrs.module.auditlogweb.api.dto.AuditFieldDiff;
import org.openmrs.module.auditlogweb.api.utils.AuditTypeMapper;
import org.openmrs.module.auditlogweb.api.utils.EnversUtils;
import org.openmrs.module.auditlogweb.api.utils.UtilClass;
import org.openmrs.module.auditlogweb.web.EnversUiHelper;
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

    private final AuditService auditService;
    private final EnversUiHelper  enversUiHelper;

    private static final String VIEW = MODULE_PATH + "/viewAudit";
    private static final String ENVERS_DISABLED_VIEW = MODULE_PATH + "/enversDisabled";

    /**
     * Handles HTTP GET requests to display audit details for a selected entity revision.
     * Validates required parameters, checks if audit logging is enabled, and fetches
     * both the current and previous entity revisions to compute and display differences.
     *
     * @param request the HttpServletRequest containing parameters: auditId, entityId, and class
     * @param model   the Spring ModelMap to add attributes for the view rendering
     * @return ModelAndView for either the audit detail view or the Envers-disabled/error view
     */
    @RequestMapping(method = RequestMethod.GET)
    public ModelAndView showForm(HttpServletRequest request, ModelMap model) {
        // Check if auditing is enabled
        if (!EnversUtils.isEnversEnabled()) {
            model.addAttribute("errorMessage", enversUiHelper.getAdminHint());
            return new ModelAndView(ENVERS_DISABLED_VIEW, model);
        }

        // Extract request parameters
        String auditIdParam = request.getParameter("auditId");
        String entityIdParam = request.getParameter("entityId");
        String className = request.getParameter("class");

        if (auditIdParam == null || entityIdParam == null || className == null ||
                auditIdParam.isEmpty() || entityIdParam.isEmpty() || className.isEmpty()) {
            model.addAttribute("errorMessage", "Please select an entity to view audit details.");
            return new ModelAndView(VIEW, model);
        }

        try {
            int auditId = Integer.parseInt(auditIdParam);
            int entityId = Integer.parseInt(entityIdParam);
            Class<?> clazz = Class.forName(className);

            // Fetch current revision with full audit info
            AuditEntity<?> auditEntity;
            try {
                auditEntity = auditService.getAuditEntityRevisionById(clazz, entityId, auditId);
            } catch (org.hibernate.ObjectNotFoundException ex) {
                model.addAttribute("errorMessage", "Audit entity not found for this revision.");
                return new ModelAndView(VIEW, model);
            }

            Object currentEntity = auditEntity.getEntity();

            // Try to fetch the previous revision
            Object oldEntity = null;
            if (auditId - 1 > 0) {
                try {
                    oldEntity = auditService.getRevisionById(clazz, entityId, auditId - 1);
                } catch (org.hibernate.ObjectNotFoundException ignored) {}
            }

            // Compute field differences
            List<AuditFieldDiff> diffs = UtilClass.computeFieldDiffs(clazz, oldEntity, currentEntity);

            // Determine edit or revision type
            String auditType = AuditTypeMapper.toHumanReadable(auditEntity.getRevisionType());

            // Add metadata and results to model
            String username = auditService.resolveUsername(auditEntity.getChangedBy());

            model.addAttribute("entityType", className.substring(className.lastIndexOf('.') + 1));
            model.addAttribute("auditType", auditType);
            model.addAttribute("changedBy", username);
            model.addAttribute("changedOn", auditEntity.getRevisionEntity().getChangedOn());
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