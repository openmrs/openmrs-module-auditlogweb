/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.auditlogweb.api.impl;

import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.auditlogweb.AuditEntity;
import org.openmrs.module.auditlogweb.api.AuditlogwebService;
import org.openmrs.module.auditlogweb.api.dao.AuditlogwebDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AuditlogwebServiceImpl extends BaseOpenmrsService implements AuditlogwebService {
	
	private final Logger log = LoggerFactory.getLogger(AuditlogwebServiceImpl.class);
	
	private final AuditlogwebDao auditlogwebDao;
	
	public AuditlogwebServiceImpl(AuditlogwebDao auditlogwebDao) {
		this.auditlogwebDao = auditlogwebDao;
	}
	
	public <T> List<AuditEntity<T>> getAllRevisions(Class<T> entityClass) {
		return auditlogwebDao.getAllRevisions(entityClass);
	}
	
	@Override
	public <T> List<AuditEntity<T>> getAllRevisions(String entityClass) {
		try{
			Class clazz = Class.forName(entityClass);
			return getAllRevisions(clazz);
		} catch (ClassNotFoundException e) {
			log.error(e.getMessage(), e);
			return new ArrayList<>();
		}
	}
	
	public <T> T getAllRevisionById(Class<T> entityClass, int entityId, int revisionId) {
		return auditlogwebDao.getRevisionById(entityClass, entityId, revisionId);
	}
}
