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
import org.openmrs.module.auditlogweb.web.dto.AuditLogDto;
import org.openmrs.module.auditlogweb.web.mapper.AuditLogDtoMapper;
import org.openmrs.module.auditlogweb.web.service.AuditLogViewService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

import static org.openmrs.module.auditlogweb.AuditlogwebConstants.MODULE_PATH;

/**
 * Controller for handling web requests related to viewing audit logs.
 *
 * <p>This controller maps to <code>/module/auditlogweb/auditlogs.form</code> and handles:
 * <ul>
 *   <li>Rendering the audit log page</li>
 *   <li>Listing all audited entity classes</li>
 *   <li>Displaying filtered and paginated audit logs</li>
 * </ul>
 *
 * <p>Supports filtering audit logs by:
 * <ul>
 *   <li>Audited entity class</li>
 *   <li>Username (who made the change)</li>
 *   <li>Date range (start and end)</li>
 * </ul>
 *
 * If Envers is not enabled in the system, an appropriate view is shown.
 */
@Controller("auditlogweb.AuditlogwebController")
@RequestMapping(value = MODULE_PATH + "/auditlogs.form")
@RequiredArgsConstructor
public class AuditlogwebController {

    private final String VIEW = MODULE_PATH + "/auditlogs";
    private final String ENVERS_DISABLED_VIEW = MODULE_PATH + "/enversDisabled";

    private final AuditService auditService;
    private final EnversUiHelper enversUiHelper;
    private final AuditLogViewService viewService;
    private final AuditLogDtoMapper dtoMapper;

    /**
     * Handles HTTP GET requests to display the main audit logs page.
     *
     * @return the name of the view that renders the audit logs page
     */
    @RequestMapping(method = RequestMethod.GET)
    public String onGet() {
        return VIEW;
    }

    /**
     * Provides a list of all entity classes annotated with {@code @Audited}.
     *
     * <p>This method is used by the view to populate the dropdown of available audited classes.
     *
     * @return a list of fully-qualified class names that are audited
     * @throws Exception if classpath scanning fails or reflection errors occur
     */
    @ModelAttribute("classes")
    protected List<String> getClasses() throws Exception {
        return UtilClass.findClassesWithAnnotation();
    }

    /**
     * Handles HTTP POST requests to display audit logs for a selected audited entity class,
     * filtered by username and date range, and paginated.
     *
     * <p>If Envers is not enabled, returns a warning view with appropriate messaging.
     * Otherwise, this method retrieves:
     * <ul>
     *   <li>Filtered and paginated audit logs for the specified class</li>
     *   <li>Total count and pagination details</li>
     *   <li>Metadata (like current class, user, date range) for the view</li>
     * </ul>
     *
     * @param domainName the fully-qualified class name of the selected audited entity
     * @param username optional username to filter audit entries by the user who made changes
     * @param startDateStr optional start date string (e.g., "2024-01-01")
     * @param endDateStr optional end date string (e.g., "2024-01-31")
     * @param page the page number for pagination (0-based)
     * @param size the number of records per page
     * @param model the model to which attributes are added for rendering the view
     * @return the name of the view to render, either audit logs or Envers-disabled notification
     */
    @RequestMapping(method = RequestMethod.POST)
    public String showClassFormAndAudits(
            @RequestParam(value = "selectedClass", required = false) String domainName,
            @RequestParam(value = "username", required = false) String username,
            @RequestParam(value = "startDate", required = false) String startDateStr,
            @RequestParam(value = "endDate", required = false) String endDateStr,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "15") int size,
            Model model) {

        if (!EnversUtils.isEnversEnabled()) {
            model.addAttribute("errorMessage", enversUiHelper.getAdminHint());
            return ENVERS_DISABLED_VIEW;
        }

        if (domainName == null || domainName.isEmpty()) {
            return VIEW;
        }

        try {
            Class<?> clazz = Class.forName(domainName);
            String simpleName = domainName.substring(domainName.lastIndexOf('.') + 1);

            LocalDate startLocal = UtilClass.parse(startDateStr);
            LocalDate endLocal = UtilClass.parse(endDateStr);

            Date startDate = UtilClass.toStartDate(startLocal);
            Date endDate = UtilClass.toEndDate(endLocal);

            List<org.openmrs.module.auditlogweb.AuditEntity<?>> audits = viewService.fetchAuditLogs(clazz, page, size, username, startDate, endDate);
            long totalCount = viewService.countAuditLogs(clazz, username, startDate, endDate);
            List<AuditLogDto> auditDtos = dtoMapper.toDtoList(audits);
            int totalPages = UtilClass.computeTotalPages(totalCount, size);

            model.addAttribute("audits", auditDtos);
            model.addAttribute("className", simpleName);
            model.addAttribute("currentClass", domainName);
            model.addAttribute("username", username);
            model.addAttribute("startDate", startDateStr);
            model.addAttribute("endDate", endDateStr);
            model.addAttribute("currentPage", page);
            model.addAttribute("pageSize", size);
            model.addAttribute("hasNextPage", (page + 1) < totalPages);
            model.addAttribute("hasPreviousPage", page > 0);
            model.addAttribute("totalCount", totalCount);
            model.addAttribute("totalPages", totalPages);

        } catch (ClassNotFoundException e) {
            model.addAttribute("errorMessage", "Class not found: " + domainName);
        }

        return VIEW;
    }
}