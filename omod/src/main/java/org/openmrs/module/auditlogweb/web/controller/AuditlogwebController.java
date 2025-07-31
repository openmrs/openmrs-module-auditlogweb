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
import org.openmrs.module.auditlogweb.web.dto.PaginatedAuditResult;
import org.openmrs.module.auditlogweb.web.mapper.AuditLogDtoMapper;
import org.openmrs.module.auditlogweb.web.service.AuditLogViewService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

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
    private final Logger log = LoggerFactory.getLogger(AuditlogwebController.class);

    private final String VIEW = MODULE_PATH + "/auditlogs";
    private final String ENVERS_DISABLED_VIEW = MODULE_PATH + "/enversDisabled";

    private final AuditService auditService;
    private final EnversUiHelper enversUiHelper;
    private final AuditLogViewService viewService;
    private final AuditLogDtoMapper dtoMapper;

    /**
     * Handles HTTP GET requests to display the default audit logs page.
     * <p>
     * This method is invoked when the audit logs page is first loaded without any filters.
     * It fetches the most recent audit log entries across all audited entities (if Envers is enabled),
     * paginates the result, and populates the model with the logs and pagination metadata
     * for rendering in the UI.
     * <p>
     * If Envers is not enabled in the system, a helpful message is shown instead.
     *
     * @param model the Spring MVC model used to pass attributes to the view
     * @return the logical view name of the audit logs JSP page
     */
    @RequestMapping(method = RequestMethod.GET)
    public String onGet(
            @RequestParam(value = "sortOrder", defaultValue = "desc") String sortOrder,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "15") int size,
            Model model) {

        if (!EnversUtils.isEnversEnabled()) {
            model.addAttribute("errorMessage", enversUiHelper.getAdminHint());
            return ENVERS_DISABLED_VIEW;
        }

        try {
            PaginatedAuditResult result = viewService.fetchAuditLogsGlobal(
                    null, null, null, null, page, size, sortOrder);

            List<AuditLogDto> audits = dtoMapper.toDtoList(result.getAudits());
            int totalPages = UtilClass.computeTotalPages(result.getTotalCount(), size);

            model.addAttribute("audits", audits);
            model.addAttribute("totalCount", result.getTotalCount());
            model.addAttribute("totalPages", totalPages);
            model.addAttribute("hasNextPage", page + 1 < totalPages);
            model.addAttribute("hasPreviousPage", page > 0);
            model.addAttribute("currentPage", page);
            model.addAttribute("pageSize", size);
            model.addAttribute("sortOrder", sortOrder);

        } catch (Exception e) {
            log.error("Failed to load default audit logs", e);
            model.addAttribute("errorMessage", "An error occurred while loading audit logs.");
        }

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
            @RequestParam(value = "sortOrder", defaultValue = "desc") String sortOrder,   // <-- New param
            Model model) {

        if (!EnversUtils.isEnversEnabled()) {
            model.addAttribute("errorMessage", enversUiHelper.getAdminHint());
            return ENVERS_DISABLED_VIEW;
        }

        try {
            PaginatedAuditResult result = viewService.fetchAuditLogsGlobal(domainName, username, startDateStr, endDateStr, page, size, sortOrder);

            List<AuditLogDto> auditDtos = dtoMapper.toDtoList(result.getAudits());
            int totalPages = UtilClass.computeTotalPages(result.getTotalCount(), size);

            model.addAttribute("audits", auditDtos);
            model.addAttribute("totalCount", result.getTotalCount());
            model.addAttribute("totalPages", totalPages);
            model.addAttribute("hasNextPage", (page + 1) < totalPages);
            model.addAttribute("hasPreviousPage", page > 0);
            model.addAttribute("username", username);
            model.addAttribute("startDate", startDateStr);
            model.addAttribute("endDate", endDateStr);
            model.addAttribute("currentPage", page);
            model.addAttribute("pageSize", size);
            model.addAttribute("sortOrder", sortOrder);

            if (domainName != null && !domainName.isEmpty()) {
                model.addAttribute("currentClass", domainName);
                String simpleName = domainName.substring(domainName.lastIndexOf('.') + 1);
                model.addAttribute("className", simpleName);
            }

        } catch (ClassNotFoundException e) {
            log.error("Class not found: {}", domainName, e);
            model.addAttribute("errorMessage", "Invalid class selected.");
        }

        return VIEW;
    }
}