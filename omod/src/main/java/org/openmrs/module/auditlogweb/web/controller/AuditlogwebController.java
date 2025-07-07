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
import org.openmrs.module.auditlogweb.api.utils.EnversUtils;
import org.openmrs.module.auditlogweb.api.utils.UtilClass;
import org.openmrs.module.auditlogweb.web.EnversUiHelper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;
import static org.openmrs.module.auditlogweb.AuditlogwebConstants.MODULE_PATH;

/**
 * Controller for managing audit log web views.
 * Handles requests to display audit logs and audited entity classes.
 */
@Controller("auditlogweb.AuditlogwebController")
@RequestMapping(value = MODULE_PATH + "/auditlogs.form")
@RequiredArgsConstructor
public class AuditlogwebController {

    private final String VIEW = MODULE_PATH + "/auditlogs";
    private final String ENVERS_DISABLED_VIEW = MODULE_PATH + "/enversDisabled";

    private final AuditService auditService;
    private final EnversUiHelper enversUiHelper;

    /**
     * Handles HTTP GET requests to display the main audit logs page.
     *
     * @return the view name for the audit logs page
     */
    @RequestMapping(method = RequestMethod.GET)
    public String onGet() {
        return VIEW;
    }

    /**
     * Provides a list of all audited entity classes annotated with @Audited.
     * This list is added to the model attribute "classes" for use in views.
     *
     * @return list of fully qualified class names of audited entities
     * @throws Exception if an error occurs while scanning classes
     */
    @ModelAttribute("classes")
    protected List<String> getClasses() throws Exception {
        return UtilClass.findClassesWithAnnotation();
    }

    /**
     * Handles HTTP POST requests to display audit logs for a selected audited entity class.
     * If Envers auditing is disabled, adds an error message and returns a special view.
     * Otherwise, fetches audit logs and pagination details, adds them to the model,
     * and returns the audit logs view.
     *
     * @param domainName the fully qualified name of the audited entity class selected
     * @param page       the page number (zero-based) for pagination
     * @param size       the number of audit logs per page
     * @param model      the Spring MVC model to populate attributes for the view
     * @return the view name to display audit logs or the Envers-disabled notification view
     */
    @RequestMapping(method = RequestMethod.POST)
    public String showClassFormAndAudits(
            @RequestParam(value = "selectedClass", required = false) String domainName,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "15") int size,
            Model model) {

        if (!EnversUtils.isEnversEnabled()) {
            model.addAttribute("errorMessage", enversUiHelper.getAdminHint());
            return ENVERS_DISABLED_VIEW;
        }

        if (domainName != null && !domainName.isEmpty()) {
            try {
                Class<?> clazz = Class.forName(domainName);
                String simpleName = domainName.substring(domainName.lastIndexOf('.') + 1);

                @SuppressWarnings("unchecked")
                List<org.openmrs.module.auditlogweb.AuditEntity<?>> audits =
                        (List<org.openmrs.module.auditlogweb.AuditEntity<?>>) (List<?>) auditService.getAllRevisions(clazz, page, size);
                long totalCount = auditService.countAllRevisions(clazz);

                // Map audit entities to DTOs with resolved usernames
                List<org.openmrs.module.auditlogweb.web.dto.AuditLogDto> auditDtos = audits.stream().map(audit -> {
                    Integer userId = audit.getChangedBy();
                    String username = auditService.resolveUsername(userId);

                    return new org.openmrs.module.auditlogweb.web.dto.AuditLogDto(
                            audit.getEntity(),
                            audit.getRevisionType(),
                            username,
                            audit.getRevisionEntity().getChangedOn(),
                            audit.getRevisionEntity()
                    );
                }).collect(java.util.stream.Collectors.toList());

                int totalPages = (int) Math.ceil((double) totalCount / size);
                boolean hasNextPage = (page + 1) < totalPages;
                boolean hasPreviousPage = page > 0;

                model.addAttribute("audits", auditDtos);
                model.addAttribute("className", simpleName);
                model.addAttribute("currentClass", domainName);
                model.addAttribute("currentPage", page);
                model.addAttribute("pageSize", size);
                model.addAttribute("hasNextPage", hasNextPage);
                model.addAttribute("hasPreviousPage", hasPreviousPage);
                model.addAttribute("totalCount", totalCount);
                model.addAttribute("totalPages", totalPages);

            } catch (ClassNotFoundException e) {
                model.addAttribute("errorMessage", "Class not found: " + domainName);
            }
        }

        return VIEW;
    }
}