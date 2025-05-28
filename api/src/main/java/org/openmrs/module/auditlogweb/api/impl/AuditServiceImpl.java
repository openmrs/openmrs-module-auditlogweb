/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * <p>
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.auditlogweb.api.impl;

import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.auditlogweb.AuditEntity;
import org.openmrs.module.auditlogweb.api.AuditService;
import org.openmrs.module.auditlogweb.api.dao.AuditlogDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class AuditServiceImpl extends BaseOpenmrsService implements AuditService {

    private final Logger log = LoggerFactory.getLogger(AuditServiceImpl.class);

    private final AuditlogDao auditlogDao;

    public AuditServiceImpl(AuditlogDao auditlogDao) {
        this.auditlogDao = auditlogDao;
    }

    public <T> List<AuditEntity<T>> getAllRevisions(Class<T> entityClass) {
        return auditlogDao.getAllRevisions(entityClass);
    }

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

    public <T> T getRevisionById(Class<T> entityClass, int entityId, int revisionId) {
        return auditlogDao.getRevisionById(entityClass, entityId, revisionId);
    }
}
