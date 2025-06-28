/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.auditlogweb.api.impl;

import lombok.RequiredArgsConstructor;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.auditlogweb.AuditEntity;
import org.openmrs.module.auditlogweb.api.AuditService;
import org.openmrs.module.auditlogweb.api.dao.AuditDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Default implementation of the {@link AuditService} interface.
 * Delegates audit-related operations to the {@link AuditDao} layer.
 */
@RequiredArgsConstructor
@Service
public class AuditServiceImpl extends BaseOpenmrsService implements AuditService {

    private final Logger log = LoggerFactory.getLogger(AuditServiceImpl.class);
    private final AuditDao auditDao;

    /**
     * Retrieves a paginated list of all audit revisions for a given audited entity class.
     *
     * @param entityClass the audited entity class
     * @param page        the page number (zero-based)
     * @param size        the number of results per page
     * @param <T>         the type of the audited entity
     * @return a list of {@link AuditEntity} containing audit revision data
     */
    @Override
    public <T> List<AuditEntity<T>> getAllRevisions(Class<T> entityClass, int page, int size) {
        return auditDao.getAllRevisions(entityClass, page, size);
    }

    /**
     * Retrieves a paginated list of all audit revisions for a given audited entity class name.
     * If the class cannot be found, returns an empty list.
     *
     * @param entityClassName fully qualified class name of the audited entity
     * @param page            the page number (zero-based)
     * @param size            the number of results per page
     * @param <T>             the type of the audited entity
     * @return a list of {@link AuditEntity} containing audit revision data or empty list if class not found
     */
    @Override
    public <T> List<AuditEntity<T>> getAllRevisions(String entityClassName, int page, int size) {
        try {
            Class<T> clazz = (Class<T>) Class.forName(entityClassName);
            return getAllRevisions(clazz, page, size);
        } catch (ClassNotFoundException e) {
            log.error("Entity class not found: {}", entityClassName, e);
            return new ArrayList<>();
        }
    }

    /**
     * Retrieves a specific revision of an audited entity by its entity ID and revision number.
     *
     * @param entityClass the audited entity class
     * @param entityId    the ID of the audited entity
     * @param revisionId  the revision number to fetch
     * @param <T>         the type of the audited entity
     * @return the entity instance at the specified revision, or {@code null} if not found
     */
    @Override
    public <T> T getRevisionById(Class<T> entityClass, int entityId, int revisionId) {
        return auditDao.getRevisionById(entityClass, entityId, revisionId);
    }

    /**
     * Retrieves an {@link AuditEntity} containing the audited entity and its revision metadata
     * by entity ID and revision number.
     *
     * @param entityClass the audited entity class
     * @param entityId    the ID of the audited entity
     * @param revisionId  the revision number to fetch
     * @param <T>         the type of the audited entity
     * @return an {@link AuditEntity} including entity and revision info, or {@code null} if not found
     */
    @Override
    public <T> AuditEntity<T> getAuditEntityRevisionById(Class<T> entityClass, int entityId, int revisionId) {
        return auditDao.getAuditEntityRevisionById(entityClass, entityId, revisionId);
    }

    /**
     * Counts the total number of revisions for a given audited entity class.
     *
     * @param entityClass the audited entity class
     * @param <T>         the type of the audited entity
     * @return the total number of revisions
     */
    @Override
    public <T> long countAllRevisions(Class<T> entityClass) {
        return auditDao.countAllRevisions(entityClass);
    }

    /**
     * Counts the total number of revisions for a given audited entity class name.
     * Returns 0 if the class cannot be found.
     *
     * @param entityClassName fully qualified class name of the audited entity
     * @return the total number of revisions, or 0 if class not found
     */
    public long countAllRevisions(String entityClassName) {
        try {
            Class<?> clazz = Class.forName(entityClassName);
            return countAllRevisions(clazz);
        } catch (ClassNotFoundException e) {
            log.error("Entity class not found: {}", entityClassName, e);
            return 0L;
        }
    }

    /**
     * Resolves a user ID to a displayable username.
     * If the user or username is missing, falls back to the user's systemId.
     * If both are missing, returns "Unknown".
     *
     * @param userId the ID of the user
     * @return the resolved username, systemId, or "Unknown" if not found
     */
    @Override
    public String resolveUsername(Integer userId) {
        if (userId == null) {
            return "Unknown";
        }

        User user = Context.getUserService().getUser(userId);
        String username = user.getUsername();
        if (username == null || username.trim().isEmpty()) {
            // fall back to systemId if username is empty
            return user.getSystemId() != null ? user.getSystemId() : "Unknown";
        }
        return username;
    }
}