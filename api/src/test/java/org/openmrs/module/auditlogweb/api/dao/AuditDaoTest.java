/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.auditlogweb.api.dao;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditQuery;
import org.hibernate.envers.query.AuditQueryCreator;
import org.hibernate.exception.SQLGrammarException;
import org.hibernate.query.Query;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.openmrs.api.db.hibernate.envers.OpenmrsRevisionEntity;
import org.openmrs.module.auditlogweb.AuditEntity;
import org.openmrs.module.auditlogweb.AuditSecurityEvent;
import org.openmrs.module.auditlogweb.api.utils.AuditSecurityEventType;
import org.openmrs.module.auditlogweb.api.exception.AuditLogUnavailableException;
import org.openmrs.module.auditlogweb.api.utils.EnversUtils;
import org.openmrs.module.auditlogweb.api.utils.UtilClass;

import java.sql.SQLException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Arrays;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

import static org.mockito.ArgumentMatchers.anyString;

class AuditDaoTest {
	
	@Mock
	private SessionFactory sessionFactory;
	
	@Mock
	private Session session;
	
	@Mock
	private AuditReader auditReader;
	
	@Mock
	private AuditQueryCreator queryCreator;
	
	@Mock
	private AuditQuery auditQuery;
	
	@Mock
	private Query<AuditSecurityEvent> securityEventQuery;
	
	@Mock
	private Query<Long> countQuery;
	
	@InjectMocks
	private AuditDao auditDao;
	
	private MockedStatic<AuditReaderFactory> readerFactoryMockedStatic;
	
	private MockedStatic<EnversUtils> enversUtilsMockedStatic;
	
	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		when(sessionFactory.getCurrentSession()).thenReturn(session);
		readerFactoryMockedStatic = mockStatic(AuditReaderFactory.class);
		readerFactoryMockedStatic.when(() -> AuditReaderFactory.get(session)).thenReturn(auditReader);
		when(auditReader.createQuery()).thenReturn(queryCreator);
		
		enversUtilsMockedStatic = mockStatic(EnversUtils.class);
	}
	
	@AfterEach
	void tearDown() {
		if (readerFactoryMockedStatic != null) {
			readerFactoryMockedStatic.close();
		}
		if (enversUtilsMockedStatic != null) {
			enversUtilsMockedStatic.close();
		}
	}
	
	@Audited
	static class TestAuditedEntity {}
	
	@Test
	void shouldReturnAuditEntities_GivenEntityClassAndPagination() {
		TestAuditedEntity entity = new TestAuditedEntity();
		OpenmrsRevisionEntity revEntity = mock(OpenmrsRevisionEntity.class);
		when(revEntity.getChangedBy()).thenReturn(42);
		Object[] mockResult = new Object[] { entity, revEntity, RevisionType.ADD };
		
		when(queryCreator.forRevisionsOfEntity(TestAuditedEntity.class, false, true)).thenReturn(auditQuery);
		when(auditQuery.addOrder(any())).thenReturn(auditQuery);
		when(auditQuery.setFirstResult(anyInt())).thenReturn(auditQuery);
		when(auditQuery.setMaxResults(anyInt())).thenReturn(auditQuery);
		when(auditQuery.getResultList()).thenReturn(Collections.singletonList(mockResult));
		
		List<AuditEntity<TestAuditedEntity>> results = auditDao.getAllRevisions(TestAuditedEntity.class, 0, 10);
		
		assertThat(results, hasSize(1));
		assertThat(results.get(0).getChangedBy(), is(42));
	}
	
	@Test
	void shouldReturnTotalRevisionCount_GivenEntityClass() {
		when(queryCreator.forRevisionsOfEntity(TestAuditedEntity.class, false, true)).thenReturn(auditQuery);
		when(auditQuery.addProjection(any())).thenReturn(auditQuery);
		when(auditQuery.getSingleResult()).thenReturn(5L);
		
		long count = auditDao.countAllRevisions(TestAuditedEntity.class);
		assertThat(count, is(5L));
	}
	
	@Test
	void shouldReturnEntityAtSpecificRevision_GivenEntityIdAndRevisionId() {
		TestAuditedEntity entity = new TestAuditedEntity();
		when(auditReader.find(TestAuditedEntity.class, 1, 10)).thenReturn(entity);
		
		TestAuditedEntity result = auditDao.getRevisionById(TestAuditedEntity.class, 1, 10);
		assertNotNull(result);
		assertSame(entity, result);
	}
	
	@Test
	void shouldReturnAuditEntityAtSpecificRevision_GivenEntityIdAndRevisionId() {
		TestAuditedEntity entity = new TestAuditedEntity();
		OpenmrsRevisionEntity revEntity = mock(OpenmrsRevisionEntity.class);
		when(revEntity.getChangedBy()).thenReturn(7);
		Object[] mockResult = new Object[] { entity, revEntity, RevisionType.MOD };
		
		when(queryCreator.forRevisionsOfEntity(TestAuditedEntity.class, false, true)).thenReturn(auditQuery);
		when(auditQuery.add(any())).thenReturn(auditQuery);
		when(auditQuery.getSingleResult()).thenReturn(mockResult);
		
		AuditEntity<TestAuditedEntity> auditEntity = auditDao.getAuditEntityRevisionById(TestAuditedEntity.class, 1, 10);
		
		assertNotNull(auditEntity);
		assertThat(auditEntity.getChangedBy(), is(7));
	}
	
	@Test
	void shouldReturnAuditEntitiesWithFilters() {
		TestAuditedEntity entity = new TestAuditedEntity();
		OpenmrsRevisionEntity revEntity = mock(OpenmrsRevisionEntity.class);
		when(revEntity.getChangedBy()).thenReturn(99);
		Object[] mockResult = new Object[] { entity, revEntity, RevisionType.ADD };
		
		when(auditQuery.getResultList()).thenReturn(Collections.singletonList(mockResult));
		enversUtilsMockedStatic.when(
		    () -> EnversUtils.buildFilteredAuditQuery(auditReader, TestAuditedEntity.class, 42, null, null, 0, 10, "desc"))
		        .thenReturn(auditQuery);
		
		List<AuditEntity<TestAuditedEntity>> results = auditDao.getRevisionsWithFilters(TestAuditedEntity.class, 0, 10, 42,
		    null, null);
		
		assertNotNull(results);
		assertThat(results, hasSize(1));
		assertThat(results.get(0).getChangedBy(), is(99));
	}
	
	@Test
	void shouldReturnEmptyList_WhenNoRevisionsWithFilters() {
		when(auditQuery.getResultList()).thenReturn(Collections.emptyList());
		enversUtilsMockedStatic.when(
		    () -> EnversUtils.buildFilteredAuditQuery(auditReader, TestAuditedEntity.class, null, null, null, 0, 10, "desc"))
		        .thenReturn(auditQuery);
		
		List<AuditEntity<TestAuditedEntity>> results = auditDao.getRevisionsWithFilters(TestAuditedEntity.class, 0, 10, null,
		    null, null);
		
		assertNotNull(results);
		assertThat(results, empty());
	}
	
	@Test
	void shouldReturnCountOfRevisionsWithFilters() {
		when(auditQuery.getSingleResult()).thenReturn(7L);
		enversUtilsMockedStatic
		        .when(() -> EnversUtils.buildCountQueryWithFilters(auditReader, TestAuditedEntity.class, 42, null, null))
		        .thenReturn(auditQuery);
		
		long count = auditDao.countRevisionsWithFilters(TestAuditedEntity.class, 42, null, null);
		
		assertThat(count, is(7L));
	}
	
	@Test
	void shouldReturnZeroCount_WhenCountRevisionsWithFiltersReturnsNull() {
		when(auditQuery.getSingleResult()).thenReturn(null);
		enversUtilsMockedStatic
		        .when(() -> EnversUtils.buildCountQueryWithFilters(auditReader, TestAuditedEntity.class, null, null, null))
		        .thenReturn(auditQuery);
		
		long count = auditDao.countRevisionsWithFilters(TestAuditedEntity.class, null, null, null);
		
		assertThat(count, is(0L));
	}
	
	@Test
	void shouldReturnAuditEntitiesAcrossAllEntities_WithPagination() {
		try (MockedStatic<UtilClass> utilClassMockedStatic = mockStatic(UtilClass.class)) {
			utilClassMockedStatic.when(UtilClass::findClassesWithAnnotation)
			        .thenReturn(Arrays.asList(TestAuditedEntity.class.getName()));
			
			TestAuditedEntity entity = new TestAuditedEntity();
			OpenmrsRevisionEntity revEntity = mock(OpenmrsRevisionEntity.class);
			when(revEntity.getChangedBy()).thenReturn(42);
			when(revEntity.getRevisionDate()).thenReturn(new Date());
			Object[] mockResult = new Object[] { entity, revEntity, RevisionType.ADD };
			
			enversUtilsMockedStatic.when(() -> EnversUtils.buildFilteredAuditQuery(auditReader, TestAuditedEntity.class,
			    null, null, null, 0, Integer.MAX_VALUE, "desc")).thenReturn(auditQuery);
			
			when(auditQuery.getResultList()).thenReturn(Collections.singletonList(mockResult));
			
			AuditEntity<TestAuditedEntity> auditEntity = new AuditEntity<>(entity, revEntity, RevisionType.ADD, 42);
			utilClassMockedStatic.when(() -> UtilClass.paginate(any(), eq(0), eq(10)))
			        .thenReturn(Collections.singletonList(auditEntity));
			
			List<AuditEntity<?>> result = auditDao.getAllRevisionsAcrossEntities(0, 10, null, null, null, "desc");
			
			assertNotNull(result);
			assertThat(result, hasSize(1));
		}
	}
	
	@Test
	void shouldReturnCountAcrossAllEntities() {
		try (MockedStatic<UtilClass> utilClassMockedStatic = mockStatic(UtilClass.class)) {
			utilClassMockedStatic.when(UtilClass::findClassesWithAnnotation)
			        .thenReturn(Arrays.asList(TestAuditedEntity.class.getName()));
			when(auditQuery.getSingleResult()).thenReturn(5L);
			enversUtilsMockedStatic
			        .when(
			            () -> EnversUtils.buildCountQueryWithFilters(auditReader, TestAuditedEntity.class, null, null, null))
			        .thenReturn(auditQuery);
			
			long result = auditDao.countRevisionsAcrossEntities(null, null, null);
			assertThat(result, is(5L));
		}
	}
	
	@Test
	void shouldReturnEmptyList_WhenNoAuditedClassesFound() {
		try (MockedStatic<UtilClass> utilClassMockedStatic = mockStatic(UtilClass.class)) {
			utilClassMockedStatic.when(UtilClass::findClassesWithAnnotation).thenReturn(Collections.emptyList());
			
			List<AuditEntity<?>> result = auditDao.getAllRevisionsAcrossEntities(0, 10, null, null, null, "desc");
			
			assertNotNull(result);
			assertThat(result, empty());
		}
	}
	
	@Test
	void shouldReturnZeroCount_WhenNoAuditedClassesFound() {
		try (MockedStatic<UtilClass> utilClassMockedStatic = mockStatic(UtilClass.class)) {
			utilClassMockedStatic.when(UtilClass::findClassesWithAnnotation).thenReturn(Collections.emptyList());
			
			long result = auditDao.countRevisionsAcrossEntities(null, null, null);
			assertThat(result, is(0L));
		}
	}
	
	@Test
	void shouldReturnFilteredAuditEntitiesByEntityType() {
		try (MockedStatic<UtilClass> utilClassMockedStatic = mockStatic(UtilClass.class)) {
			utilClassMockedStatic.when(UtilClass::findClassesWithAnnotation)
			        .thenReturn(Arrays.asList(TestAuditedEntity.class.getName()));
			
			TestAuditedEntity entity = new TestAuditedEntity();
			OpenmrsRevisionEntity revEntity = mock(OpenmrsRevisionEntity.class);
			when(revEntity.getChangedBy()).thenReturn(99);
			when(revEntity.getRevisionDate()).thenReturn(new Date());
			Object[] mockResult = new Object[] { entity, revEntity, RevisionType.ADD };
			
			enversUtilsMockedStatic.when(() -> EnversUtils.buildFilteredAuditQuery(auditReader, TestAuditedEntity.class,
			    null, null, null, 0, Integer.MAX_VALUE, "desc")).thenReturn(auditQuery);
			when(auditQuery.getResultList()).thenReturn(Collections.singletonList(mockResult));
			
			AuditEntity<?> auditEntity = new AuditEntity<>(entity, revEntity, RevisionType.ADD, 99);
			utilClassMockedStatic.when(() -> UtilClass.paginate(any(), eq(0), eq(5)))
			        .thenReturn(Collections.singletonList(auditEntity));
			
			List<AuditEntity<?>> result = auditDao.getAllRevisionsAcrossEntitiesWithEntityType(0, 5, null, null, null,
			    "TestAuditedEntity", "desc");
			
			assertNotNull(result);
			assertThat(result, hasSize(1));
			assertThat(result.get(0).getChangedBy(), is(99));
		}
	}
	
	@Test
	void shouldReturnCountOfAuditEntitiesByEntityType() {
		try (MockedStatic<UtilClass> utilClassMockedStatic = mockStatic(UtilClass.class)) {
			utilClassMockedStatic.when(UtilClass::findClassesWithAnnotation)
			        .thenReturn(Arrays.asList(TestAuditedEntity.class.getName()));
			
			when(auditQuery.getSingleResult()).thenReturn(2L);
			enversUtilsMockedStatic
			        .when(
			            () -> EnversUtils.buildCountQueryWithFilters(auditReader, TestAuditedEntity.class, null, null, null))
			        .thenReturn(auditQuery);
			
			long result = auditDao.countRevisionsAcrossEntitiesWithEntityType(null, null, null, "TestAuditedEntity");
			
			assertThat(result, is(2L));
		}
	}
	
	@Test
	void shouldReturnAuditEntities_WhenFetchingRevisionsByEntityId() {
		TestAuditedEntity entity1 = new TestAuditedEntity();
		TestAuditedEntity entity2 = new TestAuditedEntity();
		OpenmrsRevisionEntity revEntity1 = mock(OpenmrsRevisionEntity.class);
		OpenmrsRevisionEntity revEntity2 = mock(OpenmrsRevisionEntity.class);
		when(revEntity1.getChangedBy()).thenReturn(10);
		when(revEntity2.getChangedBy()).thenReturn(20);
		
		Object[] mockResult1 = new Object[] { entity1, revEntity1, RevisionType.ADD };
		Object[] mockResult2 = new Object[] { entity2, revEntity2, RevisionType.MOD };
		
		when(queryCreator.forRevisionsOfEntity(TestAuditedEntity.class, false, true)).thenReturn(auditQuery);
		when(auditQuery.add(any())).thenReturn(auditQuery);
		when(auditQuery.addOrder(any())).thenReturn(auditQuery);
		when(auditQuery.setFirstResult(0)).thenReturn(auditQuery);
		when(auditQuery.setMaxResults(10)).thenReturn(auditQuery);
		when(auditQuery.getResultList()).thenReturn(Arrays.asList(mockResult1, mockResult2));
		
		List<AuditEntity<?>> results = auditDao.getRevisionsForEntityById(1, TestAuditedEntity.class, 0, 10, "desc");
		
		assertNotNull(results);
		assertThat(results, hasSize(2));
		assertThat(results.get(0).getChangedBy(), is(10));
		assertThat(results.get(1).getChangedBy(), is(20));
	}
	
	@Test
	void shouldReturnEmptyList_WhenEntityIdNotFound() {
		when(queryCreator.forRevisionsOfEntity(TestAuditedEntity.class, false, true)).thenReturn(auditQuery);
		when(auditQuery.add(any())).thenReturn(auditQuery);
		when(auditQuery.addOrder(any())).thenReturn(auditQuery);
		when(auditQuery.setFirstResult(0)).thenReturn(auditQuery);
		when(auditQuery.setMaxResults(10)).thenReturn(auditQuery);
		when(auditQuery.getResultList()).thenReturn(Collections.emptyList());
		
		List<AuditEntity<?>> results = auditDao.getRevisionsForEntityById(999, TestAuditedEntity.class, 0, 10, "desc");
		
		assertNotNull(results);
		assertThat(results, empty());
	}
	
	@Test
	void shouldThrowAuditLogUnavailable_WhenAuditTableIsMissingForEntityId() {
		when(queryCreator.forRevisionsOfEntity(TestAuditedEntity.class, false, true)).thenReturn(auditQuery);
		when(auditQuery.add(any())).thenReturn(auditQuery);
		when(auditQuery.addOrder(any())).thenReturn(auditQuery);
		when(auditQuery.setFirstResult(0)).thenReturn(auditQuery);
		when(auditQuery.setMaxResults(10)).thenReturn(auditQuery);
		when(auditQuery.getResultList()).thenThrow(
		    new SQLGrammarException("Table TestAuditedEntity_AUD doesn't exist", new SQLException("missing table")));
		
		AuditLogUnavailableException exception = assertThrows(AuditLogUnavailableException.class,
		    () -> auditDao.getRevisionsForEntityById(1, TestAuditedEntity.class, 0, 10, "desc"));
		
		assertThat(exception.getMessage(), is("Audit history is unavailable because its audit table is missing"));
	}
	
	@Test
	void shouldThrowAuditLogUnavailable_WhenFetchingByEntityIdFails() {
		when(queryCreator.forRevisionsOfEntity(TestAuditedEntity.class, false, true)).thenReturn(auditQuery);
		when(auditQuery.add(any())).thenReturn(auditQuery);
		when(auditQuery.addOrder(any())).thenReturn(auditQuery);
		when(auditQuery.setFirstResult(0)).thenReturn(auditQuery);
		when(auditQuery.setMaxResults(10)).thenReturn(auditQuery);
		when(auditQuery.getResultList()).thenThrow(new RuntimeException("database unavailable"));
		
		AuditLogUnavailableException exception = assertThrows(AuditLogUnavailableException.class,
		    () -> auditDao.getRevisionsForEntityById(1, TestAuditedEntity.class, 0, 10, "desc"));
		
		assertThat(exception.getMessage(), is("Audit history could not be fetched, try again later"));
	}
	
	@Test
	void shouldReturnRevisionCount_WhenCountingByEntityId() {
		when(queryCreator.forRevisionsOfEntity(TestAuditedEntity.class, false, true)).thenReturn(auditQuery);
		when(auditQuery.add(any())).thenReturn(auditQuery);
		when(auditQuery.addProjection(any())).thenReturn(auditQuery);
		when(auditQuery.getSingleResult()).thenReturn(15L);
		
		long count = auditDao.countRevisionsForEntityById(1, TestAuditedEntity.class);
		
		assertThat(count, is(15L));
	}
	
	@Test
	void shouldReturnCountZero_WhenEntityIdNotFound() {
		when(queryCreator.forRevisionsOfEntity(TestAuditedEntity.class, false, true)).thenReturn(auditQuery);
		when(auditQuery.add(any())).thenReturn(auditQuery);
		when(auditQuery.addProjection(any())).thenReturn(auditQuery);
		when(auditQuery.getSingleResult()).thenReturn(0L);
		
		long count = auditDao.countRevisionsForEntityById(1, TestAuditedEntity.class);
		
		assertThat(count, is(0L));
	}
	
	@Test
	void shouldThrowAuditLogUnavailable_WhenCountingByEntityIdFails() {
		when(queryCreator.forRevisionsOfEntity(TestAuditedEntity.class, false, true)).thenReturn(auditQuery);
		when(auditQuery.add(any())).thenReturn(auditQuery);
		when(auditQuery.addProjection(any())).thenReturn(auditQuery);
		when(auditQuery.getSingleResult()).thenThrow(new RuntimeException("database unavailable"));
		
		AuditLogUnavailableException exception = assertThrows(AuditLogUnavailableException.class,
		    () -> auditDao.countRevisionsForEntityById(1, TestAuditedEntity.class));
		
		assertThat(exception.getMessage(), is("Audit history count could not be fetched, try again later"));
	}
	
	@Test
	void shouldSaveSecurityEvent_WhenValidEventIsProvided() {
		AuditSecurityEvent event = buildSecurityEvent(AuditSecurityEventType.LOGIN_SUCCESS, "admin");
		auditDao.saveSecurityEvent(event);
		
		verify(session, times(1)).save(event);
	}
	
	@Test
	void shouldReturnSecurityEvents_WhenNoFiltersProvided() {
		List<AuditSecurityEvent> expected = Collections
		        .singletonList(buildSecurityEvent(AuditSecurityEventType.LOGOUT, "nurse01"));
		
		when(session.createQuery(anyString(), eq(AuditSecurityEvent.class))).thenReturn(securityEventQuery);
		when(securityEventQuery.setFirstResult(0)).thenReturn(securityEventQuery);
		when(securityEventQuery.setMaxResults(10)).thenReturn(securityEventQuery);
		when(securityEventQuery.getResultList()).thenReturn(expected);
		
		List<AuditSecurityEvent> result = auditDao.getSecurityEvents(null, null, null, null, 0, 10);
		
		assertNotNull(result);
		assertThat(result, hasSize(1));
		assertThat(result.get(0).getEventType(), is(AuditSecurityEventType.LOGOUT));
	}
	
	@Test
	void shouldReturnFilteredSecurityEvents_WhenEventTypeAndUsernameProvided() {
		AuditSecurityEvent event = buildSecurityEvent(AuditSecurityEventType.LOGIN_FAILURE, "admin");
		
		when(session.createQuery(anyString(), eq(AuditSecurityEvent.class))).thenReturn(securityEventQuery);
		when(securityEventQuery.setParameter(eq("eventType"), eq(AuditSecurityEventType.LOGIN_FAILURE)))
		        .thenReturn(securityEventQuery);
		when(securityEventQuery.setParameter(eq("username"), eq("admin"))).thenReturn(securityEventQuery);
		when(securityEventQuery.setFirstResult(0)).thenReturn(securityEventQuery);
		when(securityEventQuery.setMaxResults(5)).thenReturn(securityEventQuery);
		when(securityEventQuery.getResultList()).thenReturn(Collections.singletonList(event));
		
		List<AuditSecurityEvent> result = auditDao.getSecurityEvents("LOGIN_FAILURE", "admin", null, null, 0, 5);
		
		assertNotNull(result);
		assertThat(result, hasSize(1));
		assertThat(result.get(0).getUsername(), is("admin"));
		
		verify(securityEventQuery).setParameter(eq("eventType"), eq(AuditSecurityEventType.LOGIN_FAILURE));
		verify(securityEventQuery).setParameter(eq("username"), eq("%admin%"));
	}
	
	@Test
	void shouldReturnSecurityEvents_WhenDateRangeProvided() {
		Date start=new Date(System.currentTimeMillis()-86_400_000); // yesterday
		Date end=new Date();
		
		when(session.createQuery(anyString(),eq(AuditSecurityEvent.class))).thenReturn(securityEventQuery);when(securityEventQuery.setParameter(eq("startDate"),eq(start))).thenReturn(securityEventQuery);when(securityEventQuery.setParameter(eq("endDate"),eq(end))).thenReturn(securityEventQuery);when(securityEventQuery.setFirstResult(0)).thenReturn(securityEventQuery);when(securityEventQuery.setMaxResults(10)).thenReturn(securityEventQuery);when(securityEventQuery.getResultList()).thenReturn(Collections.emptyList());
		
		List<AuditSecurityEvent>result=auditDao.getSecurityEvents(null,null,start,end,0,10);
		
		assertNotNull(result);assertThat(result,empty());verify(securityEventQuery).setParameter("startDate",start);verify(securityEventQuery).setParameter("endDate",end);
	}
	
	@Test
	void shouldReturnSecurityEventCount_WhenNoFiltersProvided() {
		when(session.createQuery(anyString(), eq(Long.class))).thenReturn(countQuery);
		when(countQuery.getSingleResult()).thenReturn(42L);
		
		long count = auditDao.countSecurityEvents(null, null, null, null);
		
		assertThat(count, is(42L));
	}
	
	@Test
	void shouldReturnZeroCount_WhenCountSecurityEventsReturnsNull() {
		when(session.createQuery(anyString(), eq(Long.class))).thenReturn(countQuery);
		when(countQuery.getSingleResult()).thenReturn(null);
		
		long count = auditDao.countSecurityEvents(null, null, null, null);
		
		assertThat(count, is(0L));
	}
	
	@Test
	void shouldReturnSecurityEventCount_WhenEventTypeFilterProvided() {
		when(session.createQuery(anyString(), eq(Long.class))).thenReturn(countQuery);
		when(countQuery.setParameter(eq("eventType"), eq(AuditSecurityEventType.PASSWORD_CHANGED_SUCCESS)))
		        .thenReturn(countQuery);
		when(countQuery.getSingleResult()).thenReturn(3L);
		
		long count = auditDao.countSecurityEvents("PASSWORD_CHANGED_SUCCESS", null, null, null);
		
		assertThat(count, is(3L));
		verify(countQuery).setParameter("eventType", AuditSecurityEventType.PASSWORD_CHANGED_SUCCESS);
	}
	
	@Test
	void shouldReturnSecurityEvent_WhenFoundById() {
		AuditSecurityEvent expected = AuditSecurityEvent.builder().eventType(AuditSecurityEventType.SESSION_TIMEOUT)
		        .username("user1").eventTime(new Date()).ipAddress("127.0.0.1").sessionId("test-session-id").id(99).build();
		
		when(session.createQuery(anyString(), eq(AuditSecurityEvent.class))).thenReturn(securityEventQuery);
		when(securityEventQuery.setParameter("eventId", 99)).thenReturn(securityEventQuery);
		when(securityEventQuery.uniqueResult()).thenReturn(expected);
		
		AuditSecurityEvent result = auditDao.getSecurityEventById(99);
		
		assertNotNull(result);
		assertSame(expected, result);
		assertThat(result.getId(), is(99));
	}
	
	@Test
	void shouldReturnNull_WhenSecurityEventNotFoundById() {
		when(session.createQuery(anyString(), eq(AuditSecurityEvent.class))).thenReturn(securityEventQuery);
		when(securityEventQuery.setParameter("eventId", 404)).thenReturn(securityEventQuery);
		when(securityEventQuery.uniqueResult()).thenReturn(null);
		
		AuditSecurityEvent result = auditDao.getSecurityEventById(404);
		
		assertNull(result);
	}
	
	@Test
	void shouldReturnRelatedSecurityEvents_WhenSessionIdProvided() {
		String sessionId = "sess-abc-123";
		AuditSecurityEvent e1 = buildSecurityEvent(AuditSecurityEventType.LOGIN_SUCCESS, "user1");
		AuditSecurityEvent e2 = buildSecurityEvent(AuditSecurityEventType.LOGOUT, "user1");
		
		when(session.createQuery(anyString(), eq(AuditSecurityEvent.class))).thenReturn(securityEventQuery);
		when(securityEventQuery.setParameter("sessionId", sessionId)).thenReturn(securityEventQuery);
		when(securityEventQuery.setMaxResults(5)).thenReturn(securityEventQuery);
		when(securityEventQuery.getResultList()).thenReturn(Arrays.asList(e1, e2));
		
		List<AuditSecurityEvent> result = auditDao.getRelatedSecurityEvents(sessionId, 5);
		
		assertNotNull(result);
		assertThat(result, hasSize(2));
		verify(securityEventQuery).setParameter("sessionId", sessionId);
		verify(securityEventQuery).setMaxResults(5);
	}
	
	@Test
	void shouldReturnEmptyList_WhenNoRelatedSecurityEventsFound() {
		when(session.createQuery(anyString(), eq(AuditSecurityEvent.class))).thenReturn(securityEventQuery);
		when(securityEventQuery.setParameter(anyString(), anyString())).thenReturn(securityEventQuery);
		when(securityEventQuery.setMaxResults(anyInt())).thenReturn(securityEventQuery);
		when(securityEventQuery.getResultList()).thenReturn(Collections.emptyList());
		
		List<AuditSecurityEvent> result = auditDao.getRelatedSecurityEvents("sess-ghost", 10);
		
		assertNotNull(result);
		assertThat(result, empty());
	}
	
	@Test
	void shouldBindUnknownEventType_WhenGivenSecurityEventIsInvalid() {
		when(session.createQuery(anyString(), eq(AuditSecurityEvent.class))).thenReturn(securityEventQuery);
		when(securityEventQuery.setParameter(eq("eventType"), eq(AuditSecurityEventType.UNKNOWN)))
		        .thenReturn(securityEventQuery);
		when(securityEventQuery.setFirstResult(anyInt())).thenReturn(securityEventQuery);
		when(securityEventQuery.setMaxResults(anyInt())).thenReturn(securityEventQuery);
		when(securityEventQuery.getResultList()).thenReturn(Collections.emptyList());
		
		List<AuditSecurityEvent> result = auditDao.getSecurityEvents("INVALID_EVENT", null, null, null, 0, 10);
		
		assertNotNull(result);
		assertThat(result, empty());
		verify(securityEventQuery).setParameter("eventType", AuditSecurityEventType.UNKNOWN);
	}
	
	/** Helper function to build AuditSecurityEvent for use in tests. */
	private AuditSecurityEvent buildSecurityEvent(AuditSecurityEventType type, String username) {
		return AuditSecurityEvent.builder().eventType(type).username(username).eventTime(new Date()).ipAddress("127.0.0.1")
		        .sessionId("test-session-id").build();
	}
}
