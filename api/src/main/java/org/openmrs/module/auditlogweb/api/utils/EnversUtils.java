/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.auditlogweb.api.utils;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;
import org.openmrs.api.context.Context;

import java.util.Date;

/**
 * Utility class providing helper methods related to Hibernate Envers auditing
 * and system user checks within the OpenMRS runtime context.
 */
public class EnversUtils {

    /**
     * Checks whether Hibernate Envers auditing is enabled based on the runtime
     * property {@code hibernate.integration.envers.enabled}.
     *
     * @return {@code true} if Envers auditing is enabled, {@code false} otherwise
     */
    public static boolean isEnversEnabled() {
        String value = Context.getRuntimeProperties().getProperty("hibernate.integration.envers.enabled");
        return Boolean.parseBoolean(value);
    }

    /**
     * Determines whether the currently authenticated user has the 'System Developer' role,
     * indicating administrative privileges.
     *
     * @return {@code true} if the current user is a system administrator, {@code false} otherwise
     */
    public static boolean isCurrentUserSystemAdmin() {
        return Context.isAuthenticated() && Context.getAuthenticatedUser().hasRole("System Developer");
    }

    /**
     * Builds a Hibernate Envers {@link AuditQuery} for a given entity class,
     * applying optional filters by user and date range, and paginating the results.
     *
     * @param auditReader the {@link AuditReader} instance for querying Envers data
     * @param entityClass the audited entity class
     * @param userId      optional user ID to filter by who changed the entity
     * @param startDate   optional start date for changes (inclusive)
     * @param endDate     optional end date for changes (inclusive)
     * @param page        the page number (zero-based)
     * @param size        the number of results per page
     * @param <T>         the type of the audited entity
     * @return an {@link AuditQuery} configured with filters and pagination
     */
    public static <T> AuditQuery buildFilteredAuditQuery(
            AuditReader auditReader,
            Class<T> entityClass,
            Integer userId,
            Date startDate,
            Date endDate,
            int page,
            int size,
            String sortOrder
    ) {
        AuditQuery query = auditReader.createQuery()
                .forRevisionsOfEntity(entityClass, false, true);

        if ("asc".equalsIgnoreCase(sortOrder)) {
            query.addOrder(AuditEntity.revisionNumber().asc());
        } else {
            query.addOrder(AuditEntity.revisionNumber().desc());
        }

        applyCommonFilters(query, userId, startDate, endDate);
        query.setFirstResult(page * size);
        query.setMaxResults(size);

        return query;
    }

    /**
     * Builds a Hibernate Envers count query with optional filters for user and date range.
     * This is typically used to count how many filtered revisions exist for pagination.
     *
     * @param auditReader the {@link AuditReader} instance for querying Envers data
     * @param entityClass the audited entity class
     * @param userId      optional user ID to filter revisions
     * @param startDate   optional start date for revisions (inclusive)
     * @param endDate     optional end date for revisions (inclusive)
     * @param <T>         the type of the audited entity
     * @return an {@link AuditQuery} that projects the count of filtered revisions
     */
    public static <T> AuditQuery buildCountQueryWithFilters(
            AuditReader auditReader,
            Class<T> entityClass,
            Integer userId,
            Date startDate,
            Date endDate
    ) {
        AuditQuery query = auditReader.createQuery()
                .forRevisionsOfEntity(entityClass, false, true)
                .addProjection(AuditEntity.revisionNumber().count());

        applyCommonFilters(query, userId, startDate, endDate);
        return query;
    }

    /**
     * Applies common filters (user ID and date range) to a given {@link AuditQuery}.
     * This method is reused by both query-building methods.
     *
     * @param query      the audit query to which filters will be applied
     * @param userId     optional user ID to filter by
     * @param startDate  optional start date (inclusive)
     * @param endDate    optional end date (inclusive)
     */
    private static void applyCommonFilters(AuditQuery query, Integer userId, Date startDate, Date endDate) {
        if (userId != null) {
            query.add(AuditEntity.revisionProperty("changedBy").eq(userId));
        }
        if (startDate != null) {
            query.add(AuditEntity.revisionProperty("changedOn").ge(startDate));
        }
        if (endDate != null) {
            query.add(AuditEntity.revisionProperty("changedOn").le(endDate));
        }
    }
}