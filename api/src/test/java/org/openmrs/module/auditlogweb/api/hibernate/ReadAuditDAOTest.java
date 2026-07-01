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

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openmrs.module.auditlogweb.ReadAuditEntityMetadata;
import org.openmrs.module.auditlogweb.ReadAuditLog;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ReadAuditDAOTest {
	
	@Mock
	private SessionFactory sessionFactory;
	
	@Mock
	private Session session;
	
	@InjectMocks
	private HibernateReadAuditDao readAuditDAO;
	
	@Mock
	private Query<ReadAuditLog> readAuditLogQuery;
	
	@Mock
	private Query<Long> countQuery;
	
	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		when(sessionFactory.getCurrentSession()).thenReturn(session);
		readAuditDAO = new HibernateReadAuditDao(sessionFactory);
	}
	
	@Test
	void shouldSaveSecurityEvent() {
		ReadAuditLog log = buildReadAuditLog("Patient", true, "admin");
		readAuditDAO.saveReadAuditLog(log);
		verify(session).save(log);
	}
	
	@Test
	void shouldReturnReadAuditLog_WhenNoFilterProvided() {
		List<ReadAuditLog> expected = Collections.singletonList(buildReadAuditLog("Patient", true, "admin"));
		when(session.createQuery(anyString(), eq(ReadAuditLog.class))).thenReturn(readAuditLogQuery);
		when(readAuditLogQuery.setFirstResult(0)).thenReturn(readAuditLogQuery);
		when(readAuditLogQuery.setMaxResults(10)).thenReturn(readAuditLogQuery);
		when(readAuditLogQuery.getResultList()).thenReturn(expected);
		
		List<ReadAuditLog> result = readAuditDAO.getReadAuditLogs(null, null, null, null, 0, 10);
		
		assertNotNull(result);
		assertThat(result, hasSize(1));
		assertThat(result.get(0).getEntityName(), is("Patient"));
	}
	
	@Test
	void shouldReturnReadAuditLog_WhenAllFiltersProvided() {
		List<ReadAuditLog> expected = Collections.singletonList(buildReadAuditLog("Patient", true, "admin"));
		when(session.createQuery(anyString(), eq(ReadAuditLog.class))).thenReturn(readAuditLogQuery);
		when(readAuditLogQuery.setParameter(anyString(), any())).thenReturn(readAuditLogQuery);
		when(readAuditLogQuery.setFirstResult(0)).thenReturn(readAuditLogQuery);
		when(readAuditLogQuery.setMaxResults(10)).thenReturn(readAuditLogQuery);
		when(readAuditLogQuery.getResultList()).thenReturn(expected);
		
		Date startDate = new Date();
		Date endDate = new Date();
		List<ReadAuditLog> result = readAuditDAO.getReadAuditLogs("Patient", "admin", startDate, endDate, 0, 10);
		
		assertNotNull(result);
		assertThat(result, hasSize(1));
		verify(readAuditLogQuery).setParameter("entityType", "Patient");
		verify(readAuditLogQuery).setParameter("username", "%admin%");
		verify(readAuditLogQuery).setParameter("startDate", startDate);
		verify(readAuditLogQuery).setParameter("endDate", endDate);
	}
	
	@Test
	void shouldCountReadAuditLogs_WhenFiltersProvided() {
		when(session.createQuery(anyString(), eq(Long.class))).thenReturn(countQuery);
		when(countQuery.setParameter(anyString(), any())).thenReturn(countQuery);
		when(countQuery.uniqueResult()).thenReturn(5L);
		
		Date startDate = new Date();
		Date endDate = new Date();
		long count = readAuditDAO.countReadAuditLogs("Patient", "admin", startDate, endDate);
		
		assertThat(count, is(5L));
		verify(countQuery).setParameter("entityType", "Patient");
		verify(countQuery).setParameter("username", "%admin%");
		verify(countQuery).setParameter("startDate", startDate);
		verify(countQuery).setParameter("endDate", endDate);
	}
	
	@Test
	void shouldGetReadAuditLogById() {
		ReadAuditLog expected = buildReadAuditLog("Patient", true, "admin");
		when(session.createQuery(anyString(), eq(ReadAuditLog.class))).thenReturn(readAuditLogQuery);
		when(readAuditLogQuery.setParameter(eq("id"), eq(1))).thenReturn(readAuditLogQuery);
		when(readAuditLogQuery.uniqueResult()).thenReturn(expected);
		
		ReadAuditLog result = readAuditDAO.getReadAuditLogById(1);
		
		assertNotNull(result);
		assertThat(result.getEntityName(), is("Patient"));
		verify(readAuditLogQuery).setParameter("id", 1);
	}
	
	@Test
	void shouldGetRelatedReadLogs() {
		List<ReadAuditLog> expected = Collections.singletonList(buildReadAuditLog("Patient", true, "admin"));
		when(session.createQuery(anyString(), eq(ReadAuditLog.class))).thenReturn(readAuditLogQuery);
		when(readAuditLogQuery.setParameter(eq("sessionId"), eq("session-123"))).thenReturn(readAuditLogQuery);
		when(readAuditLogQuery.setMaxResults(5)).thenReturn(readAuditLogQuery);
		when(readAuditLogQuery.getResultList()).thenReturn(expected);
		
		List<ReadAuditLog> result = readAuditDAO.getRelatedReadLogs("session-123", 5);
		
		assertNotNull(result);
		assertThat(result, hasSize(1));
		verify(readAuditLogQuery).setParameter("sessionId", "session-123");
		verify(readAuditLogQuery).setMaxResults(5);
	}
	
	private ReadAuditLog buildReadAuditLog(String entityName, boolean isReadSuccess, String username) {
		ReadAuditLog readAuditLog = ReadAuditLog.builder().entityName(entityName).isReadSuccess(isReadSuccess)
		        .username(username).userUUID("123e4567-e89b-1234-a2b3-1234567890ab").eventTime(new Date())
		        .ipAddress("127.0.0.1").userAgent("Mozilla/5.0 User Agent").sessionId("test-session").build();
		
		ReadAuditEntityMetadata readAuditEntityMetadata = ReadAuditEntityMetadata.builder().readAuditLog(readAuditLog)
		        .entityUuid("123e4567-e89b-1234-a2b3-1234567892fd").build();
		
		readAuditLog.setTargets(Collections.singletonList(readAuditEntityMetadata));
		return readAuditLog;
	}
}
