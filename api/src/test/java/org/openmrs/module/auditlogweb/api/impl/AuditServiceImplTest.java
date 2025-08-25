/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.auditlogweb.api.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.openmrs.User;
import org.openmrs.api.UserService;
import org.openmrs.api.context.Context;
import org.openmrs.module.auditlogweb.AuditEntity;
import org.openmrs.module.auditlogweb.api.dao.AuditDao;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class AuditServiceImplTest {

    @Mock
    private AuditDao auditDao;

    @InjectMocks
    private AuditServiceImpl auditService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    static class TestAuditedEntity {}

    @Test
    void shouldReturnAuditEntities_GivenValidEntityClassAndPagination() {
        AuditEntity<TestAuditedEntity> mockEntity = mock(AuditEntity.class);
        when(auditDao.getAllRevisions(TestAuditedEntity.class, 0, 5, "desc"))
                .thenReturn(Collections.singletonList(mockEntity));

        List<AuditEntity<TestAuditedEntity>> result = auditService.getAllRevisions(TestAuditedEntity.class, 0, 5, "desc");
        assertEquals(1, result.size());
        assertSame(mockEntity, result.get(0));
    }

    @Test
    void shouldReturnEmptyList_GivenInvalidEntityClassName() {
        List<?> result = auditService.getAllRevisions("non.existent.ClassName", 0, 5,  "desc");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnRevisionGivenEntityIdAndRevisionId() {
        TestAuditedEntity mockEntity = new TestAuditedEntity();
        when(auditDao.getRevisionById(TestAuditedEntity.class, 1, 2)).thenReturn(mockEntity);

        TestAuditedEntity result = auditService.getRevisionById(TestAuditedEntity.class, 1, 2);
        assertSame(mockEntity, result);
    }

    @Test
    void shouldReturnAuditEntityRevision_GivenEntityIdAndRevisionId() {
        AuditEntity<TestAuditedEntity> mockEntity = mock(AuditEntity.class);
        when(auditDao.getAuditEntityRevisionById(TestAuditedEntity.class, 1, 3)).thenReturn(mockEntity);

        AuditEntity<TestAuditedEntity> result =
                auditService.getAuditEntityRevisionById(TestAuditedEntity.class, 1, 3);
        assertSame(mockEntity, result);
    }

    @Test
    void shouldReturnTotalRevisionCount_GivenEntityClass() {
        when(auditDao.countAllRevisions(TestAuditedEntity.class)).thenReturn(10L);
        long result = auditService.countAllRevisions(TestAuditedEntity.class);
        assertEquals(10L, result);
    }

    @Test
    void shouldReturnZeroGivenInvalidEntityClassName() {
        long result = auditService.countAllRevisions("invalid.Class");
        assertEquals(0L, result);
    }

    @Test
    void shouldReturnUnknown_GivenNullUserId() {
        assertEquals("Unknown", auditService.resolveUsername(null));
    }

    @Test
    void shouldReturnUsername_GivenValidUserId() {
        try (MockedStatic<Context> context = mockStatic(Context.class)) {
            UserService userService = mock(UserService.class);
            User user = mock(User.class);

            when(user.getDisplayString()).thenReturn("Supper User (testuser)");
            context.when(Context::getUserService).thenReturn(userService);
            when(userService.getUser(10)).thenReturn(user);

            String result = auditService.resolveUsername(10);
            assertEquals("Supper User (testuser)", result);
        }
    }

    @Test
    void shouldReturnSystemId_GivenEmptyUsername() {
        try (MockedStatic<Context> context = mockStatic(Context.class)) {
            UserService userService = mock(UserService.class);
            User user = mock(User.class);

            when(user.getUsername()).thenReturn("");
            when(user.getSystemId()).thenReturn("testadmin");
            context.when(Context::getUserService).thenReturn(userService);
            when(userService.getUser(5)).thenReturn(user);

            String result = auditService.resolveUsername(5);
            assertEquals("testadmin", result);
        }
    }

    @Test
    void shouldReturnUnknown_GivenUserWithoutUsernameOrSystemId() {
        try (MockedStatic<Context> context = mockStatic(Context.class)) {
            UserService userService = mock(UserService.class);
            User user = mock(User.class);

            when(user.getUsername()).thenReturn(null);
            when(user.getSystemId()).thenReturn(null);
            context.when(Context::getUserService).thenReturn(userService);
            when(userService.getUser(8)).thenReturn(user);

            String result = auditService.resolveUsername(8);
            assertEquals("Unknown", result);
        }
    }

    @Test
    void shouldDelegateGetRevisionsWithFilters() {
        AuditEntity<TestAuditedEntity> mockEntity = mock(AuditEntity.class);
        when(auditDao.getRevisionsWithFilters(TestAuditedEntity.class, 1, 10, 2, null, null, "desc"))
                .thenReturn(Collections.singletonList(mockEntity));

        List<AuditEntity<TestAuditedEntity>> result =
                auditService.getRevisionsWithFilters(TestAuditedEntity.class, 1, 10, 2, null, null, "desc");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertSame(mockEntity, result.get(0));
    }

    @Test
    void shouldDelegateCountRevisionsWithFilters() {
        when(auditDao.countRevisionsWithFilters(TestAuditedEntity.class, 3, null, null))
                .thenReturn(15L);

        long count = auditService.countRevisionsWithFilters(TestAuditedEntity.class, 3, null, null);
        assertEquals(15L, count);
    }

    @Test
    void shouldResolveUserId_GivenMatchingUsers() {
        try (MockedStatic<Context> context = mockStatic(Context.class)) {
            UserService userService = mock(UserService.class);
            User user1 = mock(User.class);

            when(user1.getUserId()).thenReturn(99);
            context.when(Context::getUserService).thenReturn(userService);
            when(userService.getUsers("someUser", null, false)).thenReturn(Arrays.asList(user1));

            Integer userId = auditService.resolveUserId("someUser");
            assertEquals(99, userId);
        }
    }

    @Test
    void shouldReturnNullWhenNoUsersFoundOnResolveUserId() {
        try (MockedStatic<Context> context = mockStatic(Context.class)) {
            UserService userService = mock(UserService.class);
            context.when(Context::getUserService).thenReturn(userService);
            when(userService.getUsers("unknown", null, false)).thenReturn(Collections.emptyList());

            Integer userId = auditService.resolveUserId("unknown");
            assertEquals(null, userId);
        }
    }

    @Test
    void shouldReturnNull_WhenInputIsBlankInResolveUserId() {
        Integer userId = auditService.resolveUserId("");
        assertEquals(null, userId);
    }

    @Test
    void shouldReturnAuditEntitiesAcrossEntities_GivenUserIdAndDateRange() throws ParseException {
        AuditEntity<?> mockEntity = mock(AuditEntity.class);
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        Date startDate = sdf.parse("01/01/2025");
        Date endDate = sdf.parse("10/07/2025");

        when(auditDao.getAllRevisionsAcrossEntities(0, 5, 10, startDate, endDate, "desc"))
                .thenReturn(Collections.singletonList(mockEntity));

        List<AuditEntity<?>> result = auditService.getAllRevisionsAcrossEntities(0, 5, 10, startDate, endDate,  "desc");

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void shouldReturnCountAcrossEntities_GivenFixedDateRangeAndUserId() throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        Date startDate = sdf.parse("01/01/2025");
        Date endDate = sdf.parse("10/07/2025");

        when(auditDao.countRevisionsAcrossEntities(12, startDate, endDate)).thenReturn(42L);

        long count = auditService.countRevisionsAcrossEntities(12, startDate, endDate);
        assertEquals(42L, count);
    }

    @Test
    void shouldReturnAuditEntitiesAcrossEntities_GivenPaginationAndOptionalFilters() {
        AuditEntity<?> mockEntity = mock(AuditEntity.class);
        when(auditDao.getAllRevisionsAcrossEntities(0, 5, null, null, null, "desc"))
                .thenReturn(Collections.singletonList(mockEntity));

        List<AuditEntity<?>> result =
                auditService.getAllRevisionsAcrossEntities(0, 5, null, null, null, "desc");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertSame(mockEntity, result.get(0));
    }

    @Test
    void shouldReturnCountAcrossEntities_GivenUserIdAndDateRange() {
        when(auditDao.countRevisionsAcrossEntities(1, null, null)).thenReturn(25L);

        long count = auditService.countRevisionsAcrossEntities(1, null, null);
        assertEquals(25L, count);
    }

//    @Test
//    void shouldReturnAuditLogsAsDto_GivenValidAuditEntity() {
//        AuditEntity auditEntity = mock(AuditEntity.class);
//
//        TestEntity entityWithId = new TestEntity();
//        entityWithId.setId(3);
//        when(auditEntity.getEntity()).thenReturn(entityWithId);
//        when(auditEntity.getRevisionType()).thenReturn(org.hibernate.envers.RevisionType.ADD);
//        when(auditEntity.getChangedBy()).thenReturn(1);
//
//        OpenmrsRevisionEntity revisionEntity = mock(OpenmrsRevisionEntity.class);
//        Date now = new Date();
//        when(revisionEntity.getRevisionDate()).thenReturn(now);
//        when(auditEntity.getRevisionEntity()).thenReturn(revisionEntity);
//
//        try (MockedStatic<Context> context = mockStatic(Context.class)) {
//            UserService userService = mock(UserService.class);
//            User user = mock(User.class);
//            when(user.getUsername()).thenReturn("Super User");
//            when(user.getPerson()).thenReturn(null);
//            when(userService.getUser(1)).thenReturn(user);
//            context.when(Context::getUserService).thenReturn(userService);
//
//            when(auditDao.getAllRevisionsAcrossEntities(0, 3, null, null, null, "desc"))
//                    .thenReturn(Collections.singletonList(auditEntity));
//
//            RestAuditLogDto dto = new RestAuditLogDto(
//                    "Patient",
//                    "3",
//                    "ADD",
//                    "Super User",
//                    now.toString()
//            );
//            when(dtoMapper.toDtoList(anyList())).thenReturn(Collections.singletonList(dto));
//
//            List<RestAuditLogDto> result = auditService.getAllAuditLogs(0, 10);
//
//            assertNotNull(result);
//            assertEquals(1, result.size());
//            assertEquals("ADD", result.get(0).getEventType());
//            assertEquals("3", result.get(0).getEntityId());
//            assertEquals("Super User", result.get(0).getChangedBy());
//        }
//    }

    @Test
    void shouldReturnTotalAuditLogsCount() {
        when(auditDao.countRevisionsAcrossEntities(null, null, null)).thenReturn(100L);
        long count = auditService.getAuditLogsCount();
        assertEquals(100L, count);
    }

    public static class TestEntity {
        private Integer id;
        public Integer getId() {
            return id;
        }
        public void setId(Integer id) { this.id = id; }
    }

}
