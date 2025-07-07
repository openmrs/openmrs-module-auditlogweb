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

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
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

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(sessionFactory.getCurrentSession()).thenReturn(session);
        readerFactoryMockedStatic = mockStatic(AuditReaderFactory.class);
        readerFactoryMockedStatic.when(() -> AuditReaderFactory.get(session)).thenReturn(auditReader);
        when(auditReader.createQuery()).thenReturn(queryCreator);
    }

    @AfterEach
    void tearDown() {
        if (readerFactoryMockedStatic != null) {
            readerFactoryMockedStatic.close();
        }
    }

    static class TestAuditedEntity {}

    @Test
    void shouldReturnAuditEntitiesGivenEntityClassAndPagination() {
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

        assertEquals(1, results.size());
        assertEquals(42, results.get(0).getChangedBy());
    }

    @Test
    void shouldReturnTotalRevisionCountGivenEntityClass() {
        when(queryCreator.forRevisionsOfEntity(TestAuditedEntity.class, false, true)).thenReturn(auditQuery);
        when(auditQuery.addProjection(any())).thenReturn(auditQuery);
        when(auditQuery.getSingleResult()).thenReturn(5L);

        long count = auditDao.countAllRevisions(TestAuditedEntity.class);
        assertEquals(5L, count);
    }

    @Test
    void shouldReturnEntityAtSpecificRevisionGivenEntityIdAndRevisionId() {
        TestAuditedEntity entity = new TestAuditedEntity();
        when(auditReader.find(TestAuditedEntity.class, 1, 10)).thenReturn(entity);

        TestAuditedEntity result = auditDao.getRevisionById(TestAuditedEntity.class, 1, 10);
        assertNotNull(result);
        assertSame(entity, result);
    }

    @Test
    void shouldReturnAuditEntityAtSpecificRevisionGivenEntityIdAndRevisionId() {
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
        assertEquals(7, auditEntity.getChangedBy());
    }
}