/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.auditlogweb.api;

import org.openmrs.module.auditlogweb.AuditEntity;

import java.util.List;

/**
 * AuditService provides methods to retrieve audit logs for entities
 * tracked by Hibernate Envers. It allows querying historical changes,
 * revisions, and associated metadata for persisted OpenMRS domain objects.
 *
 * This service is intended for use by other modules that need to access
 * audit trail data.
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
    <T> List<AuditEntity<T>> getAllRevisions(Class<T> entityClass, int page, int size);

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
    <T> List<AuditEntity<T>> getAllRevisions(String entityClassName, int page, int size);

    /**
     * Retrieves a specific revision of an entity by its ID and revision number.
     *
     * @param entityClass the class type of the audited entity
     * @param entityId    the unique identifier of the entity
     * @param revisionId  the revision number to retrieve
     * @param <T>         the type of the audited entity
     * @return the entity instance at the specified revision, or {@code null} if not found
     */
    <T> T getRevisionById(Class<T> entityClass, int entityId, int revisionId);

    /**
     * Retrieves the full audit metadata and revision state for a specific entity revision.
     *
     * @param entityClass the class type of the audited entity
     * @param entityId    the unique identifier of the entity
     * @param revisionId  the revision number to retrieve
     * @param <T>         the type of the audited entity
     * @return an {@link AuditEntity} containing the entity, revision info, and audit metadata
     */
    <T> AuditEntity<T> getAuditEntityRevisionById(Class<T> entityClass, int entityId, int revisionId);

    /**
     * Counts the total number of revisions available for a given audited entity class.
     *
     * @param entityClass the class type of the audited entity
     * @param <T>         the type of the audited entity
     * @return the total number of revisions recorded
     */
    <T> long countAllRevisions(Class<T> entityClass);

    /**
     * Resolves the username associated with a given user ID.
     * If the user ID is null or no user is found, returns "Unknown".
     * If the username is blank, falls back to the user's system ID.
     *
     * @param userId the ID of the user to resolve
     * @return the resolved username, system ID, or "Unknown" if none available
     */
    String resolveUsername(Integer userId);
}