/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.auditlogweb.api;

import org.openmrs.annotation.Authorized;
import org.openmrs.module.auditlogweb.AuditEntity;
import org.openmrs.module.auditlogweb.api.utils.AuditLogConstants;

import java.util.List;
import java.util.Date;

/**
 * AuditService provides methods to retrieve audit logs for entities
 * tracked by Hibernate Envers. It allows querying historical changes,
 * revisions, and associated metadata for persisted OpenMRS domain objects.
 *
 * <p>This service is intended for use by other modules that need to access
 * audit trail data for entities annotated with {@code @Audited}.
 *
 * @see org.openmrs.module.auditlogweb.api.impl.AuditServiceImpl
 */
public interface AuditService {

    /**
     * Retrieves a paginated list of all revisions for the specified audited entity class.
     *
     * @param entityClass the class type of the audited entity
     * @param page        the page number (zero-based)
     * @param size        the number of results per page
     * @param <T>         the type of the audited entity
     * @return a list of {@link AuditEntity} representing revisions of the entity
     */
    @Authorized(AuditLogConstants.View_Audit_Logs)
    <T> List<AuditEntity<T>> getAllRevisions(Class<T> entityClass, int page, int size, String sortOrder);

    /**
     * Retrieves a paginated list of all revisions for the specified audited entity class
     * using its fully qualified class name.
     *
     * @param entityClassName the fully qualified name of the audited entity class
     * @param page            the page number (zero-based)
     * @param size            the number of results per page
     * @param <T>             the type of the audited entity
     * @return a list of {@link AuditEntity} representing revisions of the entity,
     *         or an empty list if the class is not found
     */
    @Authorized(AuditLogConstants.View_Audit_Logs)
    <T> List<AuditEntity<T>> getAllRevisions(String entityClassName, int page, int size, String sortOrder);

    /**
     * Retrieves a specific revision of an entity by its ID and revision number.
     *
     * @param clazz the class type of the audited entity
     * @param entityId    the unique identifier of the entity
     * @param auditId  the revision number to retrieve
     * @param <T>         the type of the audited entity
     * @return the entity instance at the specified revision, or {@code null} if not found
     */
    @Authorized(AuditLogConstants.View_Audit_Logs)
    <T> T getRevisionById(Class<T> clazz, Object entityId, int auditId);

    /**
     * Retrieves the full audit metadata and revision state for a specific entity revision.
     *
     * @param entityClass the class type of the audited entity
     * @param id          the unique identifier of the entity (Integer or String)
     * @param revisionId  the revision number to retrieve
     * @param <T>         the type of the audited entity
     * @return an {@link AuditEntity} containing the entity, revision info, and audit metadata
     */
    @Authorized(AuditLogConstants.View_Audit_Logs)
    <T> AuditEntity<T> getAuditEntityRevisionById(Class<T> entityClass, Object id, int revisionId);

    /**
     * Counts the total number of revisions available for a given audited entity class.
     *
     * @param entityClass the class type of the audited entity
     * @param <T>         the type of the audited entity
     * @return the total number of revisions recorded
     */
    <T> long countAllRevisions(Class<T> entityClass);

    /**
     * Retrieves a paginated list of revisions for a given entity class,
     * filtered by user ID and/or a date range.
     *
     * @param entityClass      the audited entity class
     * @param page       the page number (zero-based)
     * @param size       the number of records per page
     * @param userId     optional user ID to filter by who made the change (can be {@code null})
     * @param startDate  optional start date for the revision's timestamp (can be {@code null})
     * @param endDate    optional end date for the revision's timestamp (can be {@code null})
     * @param <T>        the type of the audited entity
     * @return a filtered, paginated list of {@link AuditEntity} records
     */
    @Authorized(AuditLogConstants.View_Audit_Logs)
    <T> List<AuditEntity<T>> getRevisionsWithFilters(Class<T> entityClass, int page, int size, Integer userId, Date startDate, Date endDate, String sortOrder);

    /**
     * Counts the number of revisions for a given entity class,
     * filtered by user ID and/or date range.
     *
     * @param clazz      the audited entity class
     * @param userId     optional user ID to filter by who made the change (can be {@code null})
     * @param startDate  optional start date for the revision's timestamp (can be {@code null})
     * @param endDate    optional end date for the revision's timestamp (can be {@code null})
     * @param <T>        the type of the audited entity
     * @return the number of revisions matching the filter criteria
     */
    <T> long countRevisionsWithFilters(Class<T> clazz, Integer userId, Date startDate, Date endDate);

    /**
     * Resolves the username associated with a given user ID.
     *
     * <p>If the user is not found, returns "Unknown".
     * If the username is blank or not set, falls back to returning the system ID.
     *
     * @param userId the ID of the user to resolve
     * @return the resolved username, system ID, or "Unknown" if none are available
     */
    String resolveUsername(Integer userId);

    /**
     * Resolves the numeric user ID associated with a given username.
     *
     * @param username the username to resolve
     * @return the corresponding user ID, or {@code null} if not found
     */
    Integer resolveUserId(String username);
    /**
     * Retrieves a paginated list of audit revisions across all audited entity types,
     * optionally filtered by user ID and/or date range.
     *
     * @param page       the page number (zero-based)
     * @param size       the number of records per page
     * @param userId     optional user ID to filter revisions by (can be {@code null})
     * @param startDate  optional start date to filter revisions by (can be {@code null})
     * @param endDate    optional end date to filter revisions by (can be {@code null})
     * @return a list of {@link AuditEntity} revisions from multiple entity types
     */
    @Authorized(AuditLogConstants.View_Audit_Logs)
    List<AuditEntity<?>> getAllRevisionsAcrossEntities(int page, int size, Integer userId, Date startDate, Date endDate, String sortOrder);


    /**
     * Counts the total number of audit revisions across all entity types,
     * optionally filtered by user ID and/or date range.
     *
     * @param userId     optional user ID to filter revisions by (can be {@code null})
     * @param startDate  optional start date to filter revisions by (can be {@code null})
     * @param endDate    optional end date to filter revisions by (can be {@code null})
     * @return the count of matching revisions across all entities
     */
    long countRevisionsAcrossEntities(Integer userId, Date startDate, Date endDate);
}