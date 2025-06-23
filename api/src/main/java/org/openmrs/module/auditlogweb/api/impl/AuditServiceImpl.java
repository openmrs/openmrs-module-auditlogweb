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
 * Default implementation of the {@link AuditService} interface.
 * Delegates audit-related operations to the {@link AuditDao} layer.
 */
@RequiredArgsConstructor
@Service
public class AuditServiceImpl extends BaseOpenmrsService implements AuditService {

    private final Logger log = LoggerFactory.getLogger(AuditServiceImpl.class);
    private final AuditDao auditDao;

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> List<AuditEntity<T>> getAllRevisions(Class<T> entityClass, int page, int size) {
        return auditDao.getAllRevisions(entityClass, page, size);
    }

    /**
     * Retrieves all revisions for the given entity class name (as string), paginated.
     *
     * @param entityClassName fully qualified class name of the audited entity
     * @param page            page number (zero-based)
     * @param size            number of results per page
     * @param <T>             type of the audited entity
     * @return list of {@link AuditEntity} or empty list if class not found
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
     * {@inheritDoc}
     */
    @Override
    public <T> T getRevisionById(Class<T> entityClass, int entityId, int revisionId) {
        return auditDao.getRevisionById(entityClass, entityId, revisionId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> AuditEntity<T> getAuditEntityRevisionById(Class<T> entityClass, int entityId, int revisionId) {
        return auditDao.getAuditEntityRevisionById(entityClass, entityId, revisionId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> long countAllRevisions(Class<T> entityClass) {
        return auditDao.countAllRevisions(entityClass);
    }

    /**
     * Counts total revisions for a given entity class by its fully qualified name.
     *
     * @param entityClassName the fully qualified name of the audited entity class
     * @return number of revisions, or 0 if class not found
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
}