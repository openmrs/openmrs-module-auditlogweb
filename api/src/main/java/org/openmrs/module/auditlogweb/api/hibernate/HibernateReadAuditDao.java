/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.auditlogweb.api.hibernate;

import lombok.RequiredArgsConstructor;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.openmrs.OpenmrsObject;
import org.openmrs.module.auditlogweb.ReadAuditLog;
import org.openmrs.module.auditlogweb.api.dao.ReadAuditDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository("auditlogweb.ReadAuditDao")
@RequiredArgsConstructor
public class HibernateReadAuditDao implements ReadAuditDAO {
	
	private final Logger log = LoggerFactory.getLogger(HibernateReadAuditDao.class);
	
	private final SessionFactory sessionFactory;
	
	@Override
	public void saveReadAuditLog(ReadAuditLog readAuditLog) {
		sessionFactory.getCurrentSession().save(readAuditLog);
	}
	
	@Override
	public List<ReadAuditLog> getReadAuditLogs(String entityType, String username, Date startDate, Date endDate, int page,
	        int size) {
		String hql = buildQueryConditions("from ReadAuditLog e where 1=1", entityType, username, startDate, endDate)
		        + " order by e.eventTime desc";
		
		Query<ReadAuditLog> query = sessionFactory.getCurrentSession().createQuery(hql, ReadAuditLog.class);
		
		setQueryParameters(query, entityType, username, startDate, endDate);
		
		return query.setFirstResult(page * size).setMaxResults(size).getResultList();
	}
	
	@Override
	public long countReadAuditLogs(String entityType, String username, Date startDate, Date endDate) {
		String hql = buildQueryConditions("select count(e) from ReadAuditLog e where 1=1", entityType, username, startDate,
		    endDate);
		
		Query<Long> query = sessionFactory.getCurrentSession().createQuery(hql, Long.class);
		
		setQueryParameters(query, entityType, username, startDate, endDate);
		
		Long result = query.uniqueResult();
		return result != null ? result : 0L;
	}
	
	@Override
	public ReadAuditLog getReadAuditLogById(Integer id) {
		String hql = "from ReadAuditLog e left join fetch e.targets where e.id = :id";
		Query query = sessionFactory.getCurrentSession().createQuery(hql, ReadAuditLog.class);
		query.setParameter("id", id);
		return (ReadAuditLog) query.uniqueResult();
	}
	
	public List<ReadAuditLog> getRelatedReadLogs(String sessionId, int limit) {
		Query<ReadAuditLog> query = sessionFactory.getCurrentSession().createQuery(
		    "from ReadAuditLog e where e.sessionId = :sessionId order by e.eventTime desc", ReadAuditLog.class);
		query.setParameter("sessionId", sessionId);
		query.setMaxResults(limit);
		return query.getResultList();
	}
	
	private String buildQueryConditions(String baseQuery, String entityType, String username, Date startDate, Date endDate) {
		StringBuilder hql = new StringBuilder(baseQuery);
		if (entityType != null && !entityType.trim().isEmpty()) {
			hql.append(" and e.entityName = :entityType");
		}
		if (username != null && !username.trim().isEmpty()) {
			hql.append(" and lower(e.username) like :username");
		}
		if (startDate != null) {
			hql.append(" and e.eventTime >= :startDate");
		}
		if (endDate != null) {
			hql.append(" and e.eventTime <= :endDate");
		}
		return hql.toString();
	}
	
	private void setQueryParameters(Query<?> query, String entityType, String username, Date startDate, Date endDate) {
		if (entityType != null && !entityType.trim().isEmpty()) {
			query.setParameter("entityType", entityType);
		}
		if (username != null && !username.trim().isEmpty()) {
			query.setParameter("username", "%" + username.toLowerCase() + "%");
		}
		if (startDate != null) {
			query.setParameter("startDate", startDate);
		}
		if (endDate != null) {
			query.setParameter("endDate", endDate);
		}
	}
}
