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

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import org.mockito.MockitoAnnotations;

import org.openmrs.module.auditlogweb.ModuleEvent;
import org.openmrs.module.auditlogweb.api.utils.ModuleEventType;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ModuleEventDaoTest {
	
	@Mock
	private SessionFactory sessionFactory;
	
	@Mock
	private Session session;
	
	@Mock
	private Transaction transaction;
	
	@InjectMocks
	private ModuleEventDaoImpl moduleEventDao;
	
	@Mock
	private Query<ModuleEvent> moduleEventQuery;
	
	@Mock
	private Query<Long> countQuery;
	
	@Mock
	private Query<String> stringQuery;
	
	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		when(sessionFactory.getCurrentSession()).thenReturn(session);
	}
	
	@Test
	void shouldSaveModuleLogIfSessionAndTransactionActive() {
		ModuleEvent mock = Mockito.mock(ModuleEvent.class);
		
		when(session.getTransaction()).thenReturn(transaction);
		when(session.getTransaction().isActive()).thenReturn(true);
		
		moduleEventDao.saveModuleEvent(mock);
		verify(session).save(mock);
		verify(session).flush();
		verify(sessionFactory, never()).openSession();
	}
	
	@Test
	void saveModuleEvent_NoActiveTransaction_ShouldOpenNewSessionAndCommit() {
		ModuleEvent mockEvent = Mockito.mock(ModuleEvent.class);
		when(sessionFactory.getCurrentSession()).thenReturn(session);
		when(session.getTransaction()).thenReturn(transaction);
		when(transaction.isActive()).thenReturn(false);
		
		Session newSession = Mockito.mock(Session.class);
		when(sessionFactory.openSession()).thenReturn(newSession);
		when(newSession.beginTransaction()).thenReturn(transaction);
		
		moduleEventDao.saveModuleEvent(mockEvent);
		
		verify(sessionFactory).openSession();
		verify(newSession).beginTransaction();
		verify(newSession).save(mockEvent);
		verify(transaction).commit();
		verify(newSession).close();
	}
	
	@Test
	void saveModuleEvent_GetCurrentSessionThrows_ShouldFallbackToNewSession() {
		ModuleEvent mockEvent = Mockito.mock(ModuleEvent.class);
		when(sessionFactory.getCurrentSession()).thenThrow(new HibernateException("No current session"));
		
		Session newSession = Mockito.mock(Session.class);
		when(sessionFactory.openSession()).thenReturn(newSession);
		when(newSession.beginTransaction()).thenReturn(transaction);
		
		moduleEventDao.saveModuleEvent(mockEvent);
		
		verify(newSession).save(mockEvent);
		verify(transaction).commit();
		verify(newSession).close();
	}
	
	@Test
	void saveModuleEvent_ErrorInNewSession_ShouldRollbackAndRethrow() {
		ModuleEvent mockEvent = Mockito.mock(ModuleEvent.class);
		when(sessionFactory.getCurrentSession()).thenReturn(null);
		
		Session newSession = Mockito.mock(Session.class);
		when(sessionFactory.openSession()).thenReturn(newSession);
		when(newSession.beginTransaction()).thenReturn(transaction);
		when(transaction.isActive()).thenReturn(true);
		
		doThrow(new RuntimeException("DB Connection Drop")).when(newSession).save(mockEvent);
		
		assertThrows(RuntimeException.class, () -> moduleEventDao.saveModuleEvent(mockEvent));
		
		verify(transaction, never()).commit();
		verify(transaction).rollback();
		verify(newSession).close();
		
	}
	
	@Test
	void saveModuleEvent_RollbackFails_ShouldSuppressRollbackExceptionAndRethrowOriginal() {
		ModuleEvent mockEvent = Mockito.mock(ModuleEvent.class);
		when(sessionFactory.getCurrentSession()).thenReturn(null);
		
		Session newSession = Mockito.mock(Session.class);
		when(sessionFactory.openSession()).thenReturn(newSession);
		when(newSession.beginTransaction()).thenReturn(transaction);
		when(transaction.isActive()).thenReturn(true);
		
		doThrow(new RuntimeException("Save Failed")).when(newSession).save(mockEvent);
		doThrow(new HibernateException("Rollback Failed")).when(transaction).rollback();
		
		RuntimeException exception = assertThrows(RuntimeException.class, () -> moduleEventDao.saveModuleEvent(mockEvent));
		assertEquals("Save Failed", exception.getMessage());
		
		verify(transaction, never()).commit();
		verify(newSession).close();
	}
	
	@Test
	void shouldReturnModuleEventLog_WhenNoFilterProvided() {
		
		ModuleEvent mockEvent = buildModuleEvent();
		List<ModuleEvent> moduleList = Collections.singletonList(mockEvent);
		
		when(session.createQuery(anyString(), eq(ModuleEvent.class))).thenReturn(moduleEventQuery);
		when(moduleEventQuery.setFirstResult(0)).thenReturn(moduleEventQuery);
		when(moduleEventQuery.setMaxResults(10)).thenReturn(moduleEventQuery);
		when(moduleEventQuery.list()).thenReturn(moduleList);
		
		List<ModuleEvent> result = moduleEventDao.getModuleEvents(null, null, null, null, null, null, null, null, 0, 10);
		
		assertNotNull(result);
		assertThat(result, hasSize(1));
		assertEquals("event", result.get(0).getModuleName());
	}
	
	@Test
	void shouldReturnModuleEventLog_WhenAllFilterProvided() {
		ModuleEvent mockEvent = buildModuleEvent();
		List<ModuleEvent> moduleList = Collections.singletonList(mockEvent);
		
		when(session.createQuery(anyString(), eq(ModuleEvent.class))).thenReturn(moduleEventQuery);
		when(moduleEventQuery.setFirstResult(30)).thenReturn(moduleEventQuery);
		when(moduleEventQuery.setMaxResults(2)).thenReturn(moduleEventQuery);
		when(moduleEventQuery.list()).thenReturn(moduleList);
		
		Date startDate = new Date();
		Date endDate = new Date();
		List<ModuleEvent> result = moduleEventDao.getModuleEvents(ModuleEventType.MODULE_LOAD, "Event1", "event",
		    "1.0.0-SNAPSHOT", "admin", "user-uuid-123", startDate, endDate, 15, 2);
		
		assertNotNull(result);
		assertThat(result, hasSize(1));
		assertEquals(ModuleEventType.MODULE_LOAD, result.get(0).getEventType());
		assertEquals("Event1", result.get(0).getModuleId());
		assertEquals("event", result.get(0).getModuleName());
		assertEquals("1.0.0-SNAPSHOT", result.get(0).getModuleVersion());
		assertEquals("admin", result.get(0).getUsername());
		
		verify(moduleEventQuery).setParameter("moduleName", "event");
		verify(moduleEventQuery).setParameter("eventType", ModuleEventType.MODULE_LOAD);
	}
	
	@Test
	void shouldCountModuleEvents_WhenFilterProvided() {
		when(session.createQuery(anyString(), eq(Long.class))).thenReturn(countQuery);
		when(countQuery.getSingleResult()).thenReturn(5L);
		
		long count = moduleEventDao.countModuleEvents(ModuleEventType.MODULE_LOAD, "Event1", "event", "1.0.0-SNAPSHOT",
		    "admin", "user-uuid-123", new Date(), new Date());
		
		assertThat(count, is(5L));
		verify(countQuery).setParameter("eventType", ModuleEventType.MODULE_LOAD);
		verify(countQuery).setParameter("moduleId", "Event1");
		verify(countQuery).setParameter("moduleName", "event");
		verify(countQuery).setParameter("moduleVersion", "1.0.0-SNAPSHOT");
		verify(countQuery).setParameter("username", "%admin%");
		verify(countQuery).setParameter("userUUID", "user-uuid-123");
	}
	
	@Test
	void shouldGetModuleEventById() {
		ModuleEvent mockEvent = buildModuleEvent();
		when(sessionFactory.getCurrentSession()).thenReturn(session);
		when(session.get(ModuleEvent.class, 1)).thenReturn(mockEvent);
		
		ModuleEvent event = moduleEventDao.getModuleEventById(1);
		
		assertNotNull(event);
		assertThat(event.getId(), is(1));
		verify(session).get(ModuleEvent.class, 1);
	}
	
	@Test
	void shouldGetRelatedModuleEvents() {
		ModuleEvent mockEvent = buildModuleEvent();
		List<ModuleEvent> moduleList = Collections.singletonList(mockEvent);
		
		when(sessionFactory.getCurrentSession()).thenReturn(session);
		when(session.createQuery(anyString(), eq(ModuleEvent.class))).thenReturn(moduleEventQuery);
		when(moduleEventQuery.setParameter(anyString(), anyString())).thenReturn(moduleEventQuery);
		when(moduleEventQuery.setFirstResult(0)).thenReturn(moduleEventQuery);
		when(moduleEventQuery.setMaxResults(10)).thenReturn(moduleEventQuery);
		when(moduleEventQuery.getResultList()).thenReturn(moduleList);
		
		List<ModuleEvent> result = moduleEventDao.getRelatedModuleEvents("session-123", 0, 10);
		
		assertNotNull(result);
		assertThat(result, hasSize(1));
		assertEquals("event", result.get(0).getModuleName());
		assertEquals(ModuleEventType.MODULE_LOAD, result.get(0).getEventType());
		assertEquals("admin", result.get(0).getUsername());
		verify(moduleEventQuery).setParameter("sessionId", "session-123");
	}
	
	@Test
	void shouldCountRelatedModuleEvents() {
		ModuleEvent mockEvent = buildModuleEvent();
		List<ModuleEvent> moduleList = Collections.singletonList(mockEvent);
		
		when(sessionFactory.getCurrentSession()).thenReturn(session);
		when(session.createQuery(anyString(), eq(Long.class))).thenReturn(countQuery);
		when(countQuery.setParameter(anyString(), anyString())).thenReturn(countQuery);
		when(countQuery.getSingleResult()).thenReturn(5L);
		
		Long result = moduleEventDao.countRelatedModuleEvents("session-123");
		
		assertThat(result, is(5L));
		verify(countQuery).setParameter("sessionId", "session-123");
	}
	
	private ModuleEvent buildModuleEvent() {
		return ModuleEvent.builder().id(1).eventType(ModuleEventType.MODULE_LOAD).moduleId("Event1").moduleName("event")
		        .moduleVersion("1.0.0-SNAPSHOT").eventSuccess(true).username("admin").userUUID("user-uuid-123")
		        .eventTime(new Date()).ipAddress("192.169.0.1").userAgent("user-agent-abc").sessionId("session-abc").build();
	}
	
}
