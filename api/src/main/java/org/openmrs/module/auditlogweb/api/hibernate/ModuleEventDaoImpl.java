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
import org.openmrs.module.auditlogweb.ModuleEvent;
import org.openmrs.module.auditlogweb.api.dao.ModuleEventDao;
import org.openmrs.module.auditlogweb.api.utils.ModuleEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import javax.persistence.NoResultException;
import java.util.Date;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;

@Repository("auditlogweb.ModuleActionEventDao")
@RequiredArgsConstructor
public class ModuleEventDaoImpl implements ModuleEventDao {
	
	private final Logger log = LoggerFactory.getLogger(ModuleEvent.class);
	
	private final SessionFactory sessionFactory;
	
	/**
	 * We're saving the module event log, but more importantly, we are handling the hibernate
	 * transaction fallback because during the module actions the whole context got reloaded, and we
	 * lost the transaction and session also in some case. That's why we're manually checking and
	 * opening the new session and transaction .
	 */
	@Override
	public void saveModuleEvent(ModuleEvent moduleEvent) {
		Session session = null;
		Transaction tx = null;
		boolean isNewSession = false;
		try {
			session = sessionFactory.getCurrentSession();
			if (session != null && session.getTransaction() != null && session.getTransaction().isActive()) {
				session.save(moduleEvent);
				session.flush();
			} else {
				isNewSession = true;
			}
		}
		catch (Exception e) {
			isNewSession = true;
		}
		
		if (isNewSession) {
			log.debug("No active transaction or session, opening a new session to save ModuleEvent");
			try {
				session = sessionFactory.openSession();
				tx = session.beginTransaction();
				session.save(moduleEvent);
				tx.commit();
			}
			catch (Exception e) {
				if (tx != null && tx.isActive()) {
					try {
						tx.rollback();
					}
					catch (Exception rollbackEx) {
						log.warn("Rollback failed : " + rollbackEx.getMessage());
					}
				}
				throw e;
			}
			finally {
				if (session != null) {
					session.close();
				}
			}
		}
	}
	
	@Override
	public List<ModuleEvent> getModuleEvents(ModuleEventType eventType, String moduleId, String moduleName,
	        String moduleVersion, String username, String userUUID, Date startDate, Date endDate, int page, int size) {
		
		String hql = buildModuleEventQuery(eventType, moduleId, moduleName, moduleVersion, username, userUUID, startDate,
		    endDate);
		
		Query<ModuleEvent> query = sessionFactory.getCurrentSession().createQuery(hql, ModuleEvent.class);
		bindModuleEventFilters(query, eventType, moduleId, moduleName, moduleVersion, username, userUUID, startDate,
		    endDate);
		return query.setFirstResult(page * size).setMaxResults(size).list();
		
	}
	
	@Override
	public Long countModuleEvents(ModuleEventType eventType, String moduleId, String moduleName, String moduleVersion,
	        String username, String userUUID, Date startDate, Date endDate) {
		
		String hql = "select count(e) " + buildModuleEventQuery(eventType, moduleId, moduleName, moduleVersion, username,
		    userUUID, startDate, endDate);
		
		Query<Long> query = sessionFactory.getCurrentSession().createQuery(hql, Long.class);
		bindModuleEventFilters(query, eventType, moduleId, moduleName, moduleVersion, username, userUUID, startDate,
		    endDate);
		
		try {
			Long result = query.getSingleResult();
			return result != null ? result : 0L;
		}
		catch (NoResultException e) {
			return 0L;
		}
	}
	
	@Override
	public ModuleEvent getModuleEventById(Integer moduleEventId) {
		return sessionFactory.getCurrentSession().get(ModuleEvent.class, moduleEventId);
	}
	
	@Override
	public List<ModuleEvent> getRelatedModuleEvents(String sessionId, int page, int size) {
		Query<ModuleEvent> query = sessionFactory.getCurrentSession().createQuery(
		    "from ModuleEvent e where e.sessionId = :sessionId order by e.eventTime desc", ModuleEvent.class);
		query.setParameter("sessionId", sessionId);
		query.setFirstResult(page * size);
		query.setMaxResults(size);
		return query.getResultList();
	}
	
	@Override
	public Long countRelatedModuleEvents(String sessionId) {
		Query<Long> query = sessionFactory.getCurrentSession()
		        .createQuery("select count(e) from ModuleEvent e where e.sessionId = :sessionId", Long.class);
		query.setParameter("sessionId", sessionId);
		
		try {
			return query.getSingleResult();
		}
		catch (NoResultException e) {
			return 0L;
		}
	}
	
	public String buildModuleEventQuery(ModuleEventType eventType, String moduleId, String moduleName, String moduleVersion,
	        String username, String userUUID, Date startDate, Date endDate) {
		StringBuilder hql = new StringBuilder("from ModuleEvent e where 1=1");
		
		if (eventType != null) {
			hql.append(" and e.eventType = :eventType");
		}
		if (moduleId != null && !moduleId.isEmpty()) {
			hql.append(" and e.moduleId = :moduleId");
		}
		if (moduleName != null) {
			hql.append(" and e.moduleName = :moduleName");
		}
		if (moduleVersion != null && !moduleVersion.isEmpty()) {
			hql.append(" and e.moduleVersion = :moduleVersion");
		}
		if (username != null && !username.trim().isEmpty()) {
			hql.append(" and lower(e.username) like :username");
		}
		if (userUUID != null && !userUUID.trim().isEmpty()) {
			hql.append(" and e.userUUID = :userUUID");
		}
		if (startDate != null) {
			hql.append(" and e.eventTime >= :startDate");
		}
		if (endDate != null) {
			hql.append(" and e.eventTime <= :endDate");
		}
		hql.append(" order by e.eventTime desc");
		
		return hql.toString();
	}
	
	private void bindModuleEventFilters(Query<?> query, ModuleEventType eventType, String moduleId, String moduleName,
	        String moduleVersion, String username, String userUUID, Date startDate, Date endDate) {
		if (eventType != null) {
			query.setParameter("eventType", eventType);
		}
		if (moduleId != null && !moduleId.isEmpty()) {
			query.setParameter("moduleId", moduleId);
		}
		if (moduleName != null) {
			query.setParameter("moduleName", moduleName);
		}
		if (moduleVersion != null && !moduleVersion.isEmpty()) {
			query.setParameter("moduleVersion", moduleVersion);
		}
		if (username != null && !username.trim().isEmpty()) {
			query.setParameter("username", "%" + username.trim().toLowerCase() + "%");
		}
		if (userUUID != null && !userUUID.trim().isEmpty()) {
			query.setParameter("userUUID", userUUID);
		}
		if (startDate != null) {
			query.setParameter("startDate", startDate);
		}
		if (endDate != null) {
			query.setParameter("endDate", endDate);
		}
	}
}
