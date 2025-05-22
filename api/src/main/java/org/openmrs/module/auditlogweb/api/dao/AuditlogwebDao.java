/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.auditlogweb.api.dao;

import org.hibernate.SessionFactory;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditQuery;
import org.openmrs.api.context.Context;
import org.openmrs.api.db.hibernate.envers.OpenmrsRevisionEntity;
import org.openmrs.module.auditlogweb.AuditEntity;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class AuditlogwebDao {
	
	private final SessionFactory sessionFactory;
	
	public AuditlogwebDao(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}
	
	@SuppressWarnings("unchecked")
	public <T> List<AuditEntity<T>> getAllRevisions(Class<T> entityClass) {
		AuditReader auditReader = AuditReaderFactory.get(sessionFactory.getCurrentSession());;
		AuditQuery auditQuery = auditReader.createQuery().forRevisionsOfEntity(entityClass, false, true);
		return (List<AuditEntity<T>>) auditQuery.getResultList().stream()
				.map(result -> {
					Object[] array = (Object[]) result;
					T entity = entityClass.cast(array[0]);
					OpenmrsRevisionEntity revisionEntity = (OpenmrsRevisionEntity) array[1];
					RevisionType revisionType = (RevisionType) array[2];
					String changedBy = Context.getUserService().getUser(revisionEntity.getChangedBy()).toString();
					return new AuditEntity<>(entity, revisionEntity, revisionType, changedBy);
				})
				.collect(Collectors.toList());
	}
	
	public <T> T getRevisionById(Class<T> entityClass, int entityId, int revisionId) {
		AuditReader auditReader = AuditReaderFactory.get(sessionFactory.getCurrentSession());
		T entity = auditReader.find(entityClass, entityId, revisionId);
		return entity;
	}
}
