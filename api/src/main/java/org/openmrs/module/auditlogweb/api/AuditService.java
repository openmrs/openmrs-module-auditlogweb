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
 * AuditService provides methods to retrieve audit logs of entities tracked by Hibernate Envers.
 * This is the main service exposed by the Audit Logging module and is intended to be used by other modules
 * for accessing historical change records.
 *
 * @see org.openmrs.module.auditlogweb.api.impl.AuditServiceImpl
 */
public interface AuditService {

    /**
     * Retrieves all revisions for a given entity class.
     *
     * @param entityClass the class type of the audited entity
     * @param <T>         the type of the entity
     * @return a list of {@link AuditEntity} representing all revisions of the specified entity class
     */
    <T> List<AuditEntity<T>> getAllRevisions(Class<T> entityClass);

    /**
     * Retrieves all revisions for a given entity class using its fully qualified class name.
     *
     * @param entityClass the fully qualified name of the audited entity class
     * @param <T>         the type of the entity
     * @return a list of {@link AuditEntity} representing all revisions of the specified entity class
     */
    <T> List<AuditEntity<T>> getAllRevisions(String entityClass);

    /**
     * Retrieves a specific revision of a given entity by entity ID and revision ID.
     *
     * @param entityClass the class type of the audited entity
     * @param entityId    the unique identifier of the entity
     * @param revisionId  the revision number to retrieve
     * @param <T>         the type of the entity
     * @return the entity at the specified revision, or null if not found
     */
    <T> T getRevisionById(Class<T> entityClass, int entityId, int revisionId);

    /**
     * Retrieves the full audit metadata for a specific entity revision, including who made the change,
     * what was changed, and the type of change.
     *
     * @param entityClass the class type of the audited entity
     * @param entityId    the unique identifier of the entity
     * @param revisionId  the revision number to retrieve
     * @param <T>         the type of the entity
     * @return an {@link AuditEntity} representing the audited revision and its metadata
     */
    <T> AuditEntity<T> getAuditEntityRevisionById(Class<T> entityClass, int entityId, int revisionId);
}