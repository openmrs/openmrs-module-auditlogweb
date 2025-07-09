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
import org.springframework.stereotype.Component;

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
}