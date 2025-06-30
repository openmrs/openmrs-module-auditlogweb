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

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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
    void testGetAllRevisions_DelegatesToDao() {
        AuditEntity<TestAuditedEntity> mockEntity = mock(AuditEntity.class);
        when(auditDao.getAllRevisions(TestAuditedEntity.class, 0, 5))
                .thenReturn(Arrays.asList(mockEntity));

        List<AuditEntity<TestAuditedEntity>> result = auditService.getAllRevisions(TestAuditedEntity.class, 0, 5);
        assertEquals(1, result.size());
        assertSame(mockEntity, result.get(0));
    }

    @Test
    void testGetAllRevisions_ByClassName_ReturnsEmptyListOnClassNotFound() {
        List<?> result = auditService.getAllRevisions("non.existent.ClassName", 0, 5);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetRevisionById_DelegatesToDao() {
        TestAuditedEntity mockEntity = new TestAuditedEntity();
        when(auditDao.getRevisionById(TestAuditedEntity.class, 1, 2)).thenReturn(mockEntity);

        TestAuditedEntity result = auditService.getRevisionById(TestAuditedEntity.class, 1, 2);
        assertSame(mockEntity, result);
    }

    @Test
    void testGetAuditEntityRevisionById_DelegatesToDao() {
        AuditEntity<TestAuditedEntity> mockEntity = mock(AuditEntity.class);
        when(auditDao.getAuditEntityRevisionById(TestAuditedEntity.class, 1, 3)).thenReturn(mockEntity);

        AuditEntity<TestAuditedEntity> result =
                auditService.getAuditEntityRevisionById(TestAuditedEntity.class, 1, 3);
        assertSame(mockEntity, result);
    }

    @Test
    void testCountAllRevisions_DelegatesToDao() {
        when(auditDao.countAllRevisions(TestAuditedEntity.class)).thenReturn(10L);
        long result = auditService.countAllRevisions(TestAuditedEntity.class);
        assertEquals(10L, result);
    }

    @Test
    void testCountAllRevisions_ByClassName_ReturnsZeroOnClassNotFound() {
        long result = auditService.countAllRevisions("invalid.Class");
        assertEquals(0L, result);
    }

    @Test
    void testResolveUsername_WithNullUserId_ReturnsUnknown() {
        assertEquals("Unknown", auditService.resolveUsername(null));
    }

    @Test
    void testResolveUsername_WithValidUser_ReturnsUsername() {
        try (MockedStatic<Context> context = mockStatic(Context.class)) {
            UserService userService = mock(UserService.class);
            User user = mock(User.class);

            when(user.getUsername()).thenReturn("testuser");
            context.when(Context::getUserService).thenReturn(userService);
            when(userService.getUser(10)).thenReturn(user);

            String result = auditService.resolveUsername(10);
            assertEquals("testuser", result);
        }
    }

    @Test
    void testResolveUsername_FallsBackToSystemId() {
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
    void testResolveUsername_ReturnsUnknownIfNoUsernameOrSystemId() {
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
}