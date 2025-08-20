/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.auditlogweb.rest;

import lombok.RequiredArgsConstructor;
import org.openmrs.User;
import org.openmrs.annotation.Authorized;
import org.openmrs.api.context.Context;
import org.openmrs.module.auditlogweb.api.AuditService;
import org.openmrs.module.auditlogweb.api.dto.AuditLogResponseDto;
import org.openmrs.module.auditlogweb.api.dto.RestAuditLogDto;
import org.openmrs.module.auditlogweb.api.utils.AuditLogConstants;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * REST controller for exposing audit log entries via the OpenMRS REST API.
 * <p>
 * Provides endpoints to fetch audit logs with optional filtering by user, date range,
 * and entity type. Supports pagination.
 * </p>
 * <p>
 * Security: Access to the logs is controlled by the {@link AuditLogConstants#VIEW_AUDIT_LOGS} privilege.
 * </p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/rest/" + RestConstants.VERSION_1 + "/auditlogs")
public class AuditLogRestController {

    private final AuditService auditService;

    /**
     * Retrieves a paginated list of audit log entries.
     *
     * @param page       the page index (0-based). Defaults to 0 if negative.
     * @param size       the number of records per page. Defaults to 20 if zero or negative.
     * @param userId     optional user ID to filter logs by a specific user
     * @param username   optional username to filter logs by a specific user; overrides userId if provided
     * @param startDate  optional start date in "dd/MM/yyyy" format
     * @param endDate    optional end date in "dd/MM/yyyy" format; if missing and startDate is provided, defaults to today
     * @param entityType optional entity type to filter logs by entity class name
     * @return an {@link AuditLogResponseDto} containing the total logs, current page, total pages, and list of audit logs
     */
    @GetMapping
    @Authorized(AuditLogConstants.VIEW_AUDIT_LOGS)
    public AuditLogResponseDto getAllAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Integer userId,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String entityType
    ) {
        if (page < 0) page = 0;
        if (size <= 0) size = 20;

        Integer effectiveUserId = userId;
        if (effectiveUserId == null && username != null && !username.isEmpty()) {
            User u = Context.getUserService().getUserByUsername(username);
            if (u == null) {
                return new AuditLogResponseDto(0, page, 0, Collections.emptyList());
            }
            effectiveUserId = u.getUserId();
        }
        Date start = parseDate(startDate);
        Date end = parseDate(endDate);
        List<RestAuditLogDto> logs =
                auditService.getAllAuditLogs(page, size, effectiveUserId, start, end, entityType);

        long total = auditService.getAuditLogsCount(effectiveUserId, start, end, entityType);
        int totalPages = (int) Math.ceil(total / (double) size);

        return new AuditLogResponseDto(Math.toIntExact(total), page, totalPages, logs);
    }

    /**
     * Parses a date string in "dd/MM/yyyy" format.
     *
     * @param dateStr the date string to parse
     * @return the parsed {@link Date} object, or null if the input is null or empty
     * @throws RuntimeException if the date string cannot be parsed
     */
    private Date parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return null;
        try {
            return new SimpleDateFormat("dd/MM/yyyy").parse(dateStr);
        } catch (ParseException e) {
            throw  new RuntimeException(e);
        }
    }
}
