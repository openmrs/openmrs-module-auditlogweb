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
import org.openmrs.module.auditlogweb.api.dto.AuditLogDetailDTO;
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
import java.util.stream.Collectors;

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
     * REST controller for exposing audit log entries via the OpenMRS REST API.
     * <p>
     * Provides endpoints to fetch audit logs with optional filtering by:
     * - user ID or username
     * - date range (startDate/endDate in "dd/MM/yyyy" format)
     * - entity type
     * </p>
     * <p>
     * Supports pagination via 'page' (0-based) and 'size' parameters.
     * </p>
     * <p>
     * Security: Access to the logs is controlled by the {@link AuditLogConstants#VIEW_AUDIT_LOGS} privilege.
     * </p>
     */
    @GetMapping
    @Authorized(AuditLogConstants.VIEW_AUDIT_LOGS)
    public AuditLogResponseDto getAuditLogs(
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

        Integer effectiveUserId = resolveUserId(userId, username);
        Date start = parseDate(startDate);
        Date end = parseDate(endDate);

        boolean fullDetails = userId != null || username != null || startDate != null || endDate != null || entityType != null;

        List<AuditLogDetailDTO> auditDetails = auditService.mapAuditEntitiesToDetails(
                auditService.getAllRevisionsAcrossEntities(page, size, effectiveUserId, start, end, "desc")
                        .stream()
                        .filter(a -> entityType == null || a.getEntity().getClass().getSimpleName().equals(entityType))
                        .collect(Collectors.toList())
        );

        if (!fullDetails) {
            auditDetails.forEach(d -> d.setChanges(Collections.emptyList()));
        }

        long total = auditService.getAuditLogsCount(effectiveUserId, start, end, entityType);
        int totalPages = (int) Math.ceil(total / (double) size);

        return new AuditLogResponseDto(Math.toIntExact(total), page, totalPages, auditDetails);
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

    private Integer resolveUserId(Integer userId, String username) {
        if (userId != null) return userId;
        if (username != null && !username.isEmpty()) {
            User u = Context.getUserService().getUserByUsername(username);
            if (u != null) return u.getUserId();
        }
        return null;
    }
}
