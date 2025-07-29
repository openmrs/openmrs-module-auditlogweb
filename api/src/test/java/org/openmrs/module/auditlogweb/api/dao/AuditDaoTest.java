/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.openmrs.api.db.hibernate.envers.OpenmrsRevisionEntity;
import org.openmrs.module.auditlogweb.AuditEntity;
import org.openmrs.module.auditlogweb.api.utils.EnversUtils;
import org.openmrs.module.auditlogweb.api.utils.UtilClass;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Arrays;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

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

        AuditEntity<TestAuditedEntity> auditEntity =
                auditDao.getAuditEntityRevisionById(TestAuditedEntity.class, 1, 10);

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
        enversUtilsMockedStatic.when(() -> EnversUtils.buildFilteredAuditQuery(
                        auditReader, TestAuditedEntity.class, 42, null, null, 0, 10, "desc"))
                .thenReturn(auditQuery);

        List<AuditEntity<TestAuditedEntity>> results = auditDao.getRevisionsWithFilters(
                TestAuditedEntity.class, 0, 10, 42, null, null);

        assertNotNull(results);
        assertThat(results, hasSize(1));
        assertThat(results.get(0).getChangedBy(), is(99));
    }

    @Test
    void shouldReturnEmptyList_WhenNoRevisionsWithFilters() {
        when(auditQuery.getResultList()).thenReturn(Collections.emptyList());
        enversUtilsMockedStatic.when(() -> EnversUtils.buildFilteredAuditQuery(
                        auditReader, TestAuditedEntity.class, null, null, null, 0, 10, "desc"))
                .thenReturn(auditQuery);

        List<AuditEntity<TestAuditedEntity>> results = auditDao.getRevisionsWithFilters(
                TestAuditedEntity.class, 0, 10, null, null, null);

        assertNotNull(results);
        assertThat(results, empty());
    }

    @Test
    void shouldReturnCountOfRevisionsWithFilters() {
        when(auditQuery.getSingleResult()).thenReturn(7L);
        enversUtilsMockedStatic.when(() -> EnversUtils.buildCountQueryWithFilters(
                        auditReader, TestAuditedEntity.class, 42, null, null))
                .thenReturn(auditQuery);

        long count = auditDao.countRevisionsWithFilters(TestAuditedEntity.class, 42, null, null);

        assertThat(count, is(7L));
    }

    @Test
    void shouldReturnZeroCount_WhenCountRevisionsWithFiltersReturnsNull() {
        when(auditQuery.getSingleResult()).thenReturn(null);
        enversUtilsMockedStatic.when(() -> EnversUtils.buildCountQueryWithFilters(
                        auditReader, TestAuditedEntity.class, null, null, null))
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

            enversUtilsMockedStatic.when(() -> EnversUtils.buildFilteredAuditQuery(
                            auditReader, TestAuditedEntity.class, null, null, null, 0, Integer.MAX_VALUE, "desc"))
                    .thenReturn(auditQuery);

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
            enversUtilsMockedStatic.when(() -> EnversUtils.buildCountQueryWithFilters(
                            auditReader, TestAuditedEntity.class, null, null, null))
                    .thenReturn(auditQuery);

            long result = auditDao.countRevisionsAcrossEntities(null, null, null);
            assertThat(result, is(5L));
        }
    }

    @Test
    void shouldReturnEmptyList_WhenNoAuditedClassesFound() {
        try (MockedStatic<UtilClass> utilClassMockedStatic = mockStatic(UtilClass.class)) {
            utilClassMockedStatic.when(UtilClass::findClassesWithAnnotation)
                    .thenReturn(Collections.emptyList());

            List<AuditEntity<?>> result = auditDao.getAllRevisionsAcrossEntities(0, 10, null, null, null, "desc");

            assertNotNull(result);
            assertThat(result, empty());
        }
    }

    @Test
    void shouldReturnZeroCount_WhenNoAuditedClassesFound() {
        try (MockedStatic<UtilClass> utilClassMockedStatic = mockStatic(UtilClass.class)) {
            utilClassMockedStatic.when(UtilClass::findClassesWithAnnotation)
                    .thenReturn(Collections.emptyList());

            long result = auditDao.countRevisionsAcrossEntities(null, null, null);
            assertThat(result, is(0L));
        }
    }
}