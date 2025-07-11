/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.auditlogweb.web.service;

import lombok.RequiredArgsConstructor;
import org.openmrs.module.auditlogweb.AuditEntity;
import org.openmrs.module.auditlogweb.api.AuditService;
import org.openmrs.module.auditlogweb.api.utils.UtilClass;
import org.openmrs.module.auditlogweb.web.dto.AuditFilter;
import org.openmrs.module.auditlogweb.web.dto.PaginatedAuditResult;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Service class responsible for preparing audit log data for display in the UI.
 *
 * <p>This class delegates audit data retrieval to {@link AuditService},
 * and applies additional filtering logic such as resolving user IDs
 * from usernames, filtering by date ranges, and applying pagination.
 */
@Component
@RequiredArgsConstructor
public class AuditLogViewService {

    private final AuditService auditService;

    /**
     * Retrieves a list of audit log entries for the given audited entity class,
     * optionally filtered by user, start date, and end date, and paginated.
     *
     * @param clazz      the audited entity class
     * @param page       the page number (0-based index) for pagination
     * @param size       the number of records per page
     * @param username   optional username to filter by user who made the change
     * @param startDate  optional start date for filtering changes
     * @param endDate    optional end date for filtering changes
     * @return a paginated list of {@link AuditEntity} entries, possibly filtered
     */
    public List<AuditEntity<?>> fetchAuditLogs(Class<?> clazz, int page, int size, String username, Date startDate, Date endDate) {
        Integer userId = resolveUserId(username);
        boolean hasFilters = userId != null || startDate != null || endDate != null;

        if (hasFilters) {
            return (List<AuditEntity<?>>)(List<?>) auditService.getRevisionsWithFilters(clazz, page, size, userId, startDate, endDate);
        }
        return (List<AuditEntity<?>>)(List<?>) auditService.getAllRevisions(clazz, page, size);
    }

    /**
     * Counts the total number of audit log entries for the given audited entity class,
     * optionally filtered by user and date range.
     *
     * @param clazz      the audited entity class
     * @param username   optional username to filter by user who made the change
     * @param startDate  optional start date for filtering changes
     * @param endDate    optional end date for filtering changes
     * @return the total number of audit entries matching the criteria
     */
    public long countAuditLogs(Class<?> clazz, String username, Date startDate, Date endDate) {
        Integer userId = resolveUserId(username);
        boolean hasFilters = userId != null || startDate != null || endDate != null;

        if (hasFilters) {
            return auditService.countRevisionsWithFilters(clazz, userId, startDate, endDate);
        }
        return auditService.countAllRevisions(clazz);
    }

    /**
     * Resolves a user's ID from their username using the {@link AuditService}.
     *
     * @param username the username to resolve
     * @return the corresponding user ID, or {@code null} if not found or blank
     */
    private Integer resolveUserId(String username) {
        if (username == null || username.trim().isEmpty()) return null;
        return auditService.resolveUserId(username);
    }

    /**
     * Combined method to fetch audit logs either for a specific audited entity class
     * or across all audited entities (global search) with optional filters.
     *
     * @param domainClassName fully qualified class name of audited entity (nullable)
     * @param username       optional username to filter by
     * @param startDateStr   optional start date string (e.g. "2024-01-01")
     * @param endDateStr     optional end date string (e.g. "2024-01-31")
     * @param page           zero-based page index
     * @param size           page size (number of records per page)
     * @return PaginatedAuditResult containing list of AuditEntity and total count
     * @throws ClassNotFoundException if domainClassName is invalid
     */
    public PaginatedAuditResult fetchAuditLogsGlobal(
            String domainClassName,
            String username,
            String startDateStr,
            String endDateStr,
            int page,
            int size) throws ClassNotFoundException {

        AuditFilter filters = parseFilters(username, startDateStr, endDateStr);

        if (domainClassName != null && !domainClassName.isEmpty()) {
            Class<?> clazz = Class.forName(domainClassName);
            List<AuditEntity<?>> audits = fetchAuditLogs(clazz, page, size, username, filters.getStartDate(), filters.getEndDate());
            long totalCount = countAuditLogs(clazz, username, filters.getStartDate(), filters.getEndDate());
            return new PaginatedAuditResult(audits, totalCount);
        } else if (filters.getUserId() != null || filters.getStartDate() != null || filters.getEndDate() != null) {
            List<AuditEntity<?>> audits = auditService.getAllRevisionsAcrossEntities(page, size, filters.getUserId(), filters.getStartDate(), filters.getEndDate());
            long totalCount = auditService.countRevisionsAcrossEntities(filters.getUserId(), filters.getStartDate(), filters.getEndDate());
            return new PaginatedAuditResult(audits, totalCount);
        } else {
            return new PaginatedAuditResult(Collections.emptyList(), 0);
        }
    }

    /**
     * Parses raw filter input strings into an AuditFilter object.
     * Resolves username to userId and parses dates.
     *
     * @param username     username filter string
     * @param startDateStr start date string in ISO format or null
     * @param endDateStr   end date string in ISO format or null
     * @return AuditFilter DTO with parsed userId and Date fields
     */
    private AuditFilter parseFilters(String username, String startDateStr, String endDateStr) {
        AuditFilter filter = new AuditFilter();
        filter.setUserId(auditService.resolveUserId(username));
        filter.setStartDate(UtilClass.toStartDate(UtilClass.parse(startDateStr)));
        filter.setEndDate(UtilClass.toEndDate(UtilClass.parse(endDateStr)));
        return filter;
    }
}