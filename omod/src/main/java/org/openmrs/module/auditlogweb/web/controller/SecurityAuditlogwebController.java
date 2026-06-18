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
import org.openmrs.api.APIAuthenticationException;
import org.openmrs.module.auditlogweb.AuditSecurityEvent;
import org.openmrs.module.auditlogweb.api.AuditService;
import org.openmrs.api.context.Context;
import org.openmrs.module.auditlogweb.api.utils.AuditLogConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.openmrs.module.auditlogweb.AuditlogwebConstants.MODULE_PATH;

/**
 * Controller for the security audit logs UI.
 */
@Controller("auditlogweb.SecurityAuditlogwebController")
@RequestMapping(value = MODULE_PATH + "/securityauditlogs.form")
@RequiredArgsConstructor
public class SecurityAuditlogwebController {

    private static final Logger log = LoggerFactory.getLogger(SecurityAuditlogwebController.class);

    private static final String VIEW = MODULE_PATH + "/securityauditlogs";
    private final String ACCESS_DENIED_VIEW = MODULE_PATH + "/accessDenied";

    private final AuditService auditService;

    @RequestMapping(method = { RequestMethod.GET, RequestMethod.POST })
    public String onView(
            @RequestParam(value = "eventType", required = false) String eventType,
            @RequestParam(value = "username", required = false) String username,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "15") int size,
            Model model) {

        Date start = parseStartDate(startDate);
        Date end = parseEndDate(endDate);

        try {

            List<AuditSecurityEvent> events = auditService.getSecurityEvents(eventType, username, start, end, page, size);
            long totalCount = auditService.countSecurityEvents(eventType, username, start, end);
            int totalPages = (int) Math.ceil((double) totalCount / Math.max(size, 1));

            model.addAttribute("events", events);
            model.addAttribute("totalCount", totalCount);
            model.addAttribute("totalPages", totalPages);
            model.addAttribute("hasNextPage", page + 1 < totalPages);
            model.addAttribute("hasPreviousPage", page > 0);
            model.addAttribute("currentPage", page);
            model.addAttribute("pageSize", size);
            model.addAttribute("eventType", eventType);
            model.addAttribute("usernameFilter", username);
            model.addAttribute("startDate", startDate);
            model.addAttribute("endDate", endDate);
            model.addAttribute("page", "securityauditlogs");
            model.addAttribute("eventTypes", getEventTypes());
        } catch(APIAuthenticationException e){
            return ACCESS_DENIED_VIEW;
        } catch (Exception e) {
            log.error("Failed to load security audit logs", e);
            model.addAttribute("errorMessage", "An error occurred while loading security audit logs.");
            model.addAttribute("events", Arrays.asList());
            model.addAttribute("eventTypes", getEventTypes());
            model.addAttribute("page", "securityauditlogs");
        }

        return VIEW;
    }

    private List<String> getEventTypes() {
        return Arrays.asList(
                "LOGIN_SUCCESS",
                "LOGIN_FAILURE",
                "ACCOUNT_LOCKED",
                "LOGOUT",
                "SESSION_TIMEOUT",
                "PASSWORD_RESET_REQUEST",
                "PASSWORD_RESET",
                "PASSWORD_CHANGED");
    }

    private Date parseStartDate(String startDate) {
        if (startDate == null || startDate.trim().isEmpty()) {
            return null;
        }

        LocalDate localDate = LocalDate.parse(startDate);
        LocalDateTime startOfDay = localDate.atStartOfDay();
        return Date.from(startOfDay.atZone(ZoneId.systemDefault()).toInstant());
    }

    private Date parseEndDate(String endDate) {
        if (endDate == null || endDate.trim().isEmpty()) {
            return null;
        }

        LocalDate localDate = LocalDate.parse(endDate);
        LocalDateTime endOfDay = localDate.atTime(23, 59, 59);
        return Date.from(endOfDay.atZone(ZoneId.systemDefault()).toInstant());
    }
}
