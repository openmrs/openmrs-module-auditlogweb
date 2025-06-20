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
 * Implementation of the {@link AuditService} interface.
 * Provides services for retrieving audit logs and revision information
 * using the {@link AuditDao} layer.
 */
@RequiredArgsConstructor
@Service
public class AuditServiceImpl extends BaseOpenmrsService implements AuditService {

    private final Logger log = LoggerFactory.getLogger(AuditServiceImpl.class);

    private final AuditDao auditDao;

    /**
     * Retrieves all revision entries for the specified audited entity class.
     *
     * @param entityClass the audited entity class
     * @param <T>         the type of the audited entity
     * @return a list of {@link AuditEntity} containing revision data
     */
    public <T> List<AuditEntity<T>> getAllRevisions(Class<T> entityClass) {
        return auditDao.getAllRevisions(entityClass);
    }

    /**
     * Retrieves all revision entries for the specified audited entity class, given as a fully qualified class name.
     *
     * @param entityClass the fully qualified class name of the audited entity
     * @param <T>         the type of the audited entity
     * @return a list of {@link AuditEntity} containing revision data,
     *         or an empty list if the class is not found
     */
    @Override
    public <T> List<AuditEntity<T>> getAllRevisions(String entityClass) {
        try {
            Class clazz = Class.forName(entityClass);
            return getAllRevisions(clazz);
        } catch (ClassNotFoundException e) {
            log.error(e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Retrieves a specific revision of an audited entity by its ID and revision number.
     *
     * @param entityClass the audited entity class
     * @param entityId    the ID of the audited entity
     * @param revisionId  the revision number to retrieve
     * @param <T>         the type of the audited entity
     * @return the entity at the specified revision, or null if not found
     */
    public <T> T getRevisionById(Class<T> entityClass, int entityId, int revisionId) {
        return auditDao.getRevisionById(entityClass, entityId, revisionId);
    }

    /**
     * Retrieves a specific {@link AuditEntity} for an entity and revision number, including audit metadata.
     *
     * @param entityClass the audited entity class
     * @param entityId    the ID of the entity
     * @param revisionId  the revision number
     * @param <T>         the type of the audited entity
     * @return an {@link AuditEntity} containing the entity and its audit metadata
     */
    @Override
    public <T> AuditEntity<T> getAuditEntityRevisionById(Class<T> entityClass, int entityId, int revisionId) {
        return auditDao.getAuditEntityRevisionById(entityClass, entityId, revisionId);
    }
}