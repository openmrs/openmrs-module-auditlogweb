/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.auditlogweb.api.utils;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.query.AuditQuery;
import org.hibernate.envers.query.AuditQueryCreator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.openmrs.User;
import org.openmrs.api.context.Context;

import java.util.Date;
import java.util.Properties;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;

public class EnversUtilsTest {

    private MockedStatic<Context> mockedContext;
    private AuditReader auditReader;
    private AuditQueryCreator queryCreator;
    private AuditQuery auditQuery;

    @Before
    public void setUp() {
        mockedContext = mockStatic(Context.class);

        auditReader = mock(AuditReader.class);
        queryCreator = mock(AuditQueryCreator.class);
        auditQuery = mock(AuditQuery.class);
    }

    @After
    public void tearDown() {
        mockedContext.close();
    }

    @Test
    public void isEnversEnabled_shouldReturnTrue() {
        Properties props = mock(Properties.class);
        mockedContext.when(Context::getRuntimeProperties).thenReturn(props);
        when(props.getProperty("hibernate.integration.envers.enabled")).thenReturn("true");

        assertTrue(EnversUtils.isEnversEnabled());
    }

    @Test
    public void isEnversEnabled_shouldReturnFalseIfUnset() {
        Properties props = mock(Properties.class);
        mockedContext.when(Context::getRuntimeProperties).thenReturn(props);
        when(props.getProperty("hibernate.integration.envers.enabled")).thenReturn(null);

        assertFalse(EnversUtils.isEnversEnabled());
    }

    @Test
    public void isCurrentUserSystemAdmin_shouldReturnTrueIfUserHasRole() {
        User user = mock(User.class);
        mockedContext.when(Context::isAuthenticated).thenReturn(true);
        mockedContext.when(Context::getAuthenticatedUser).thenReturn(user);
        when(user.hasRole("System Developer")).thenReturn(true);

        assertTrue(EnversUtils.isCurrentUserSystemAdmin());
    }

    @Test
    public void isCurrentUserSystemAdmin_shouldReturnFalseIfNotAuthenticated() {
        mockedContext.when(Context::isAuthenticated).thenReturn(false);
        assertFalse(EnversUtils.isCurrentUserSystemAdmin());
    }

    @Test
    public void buildFilteredAuditQuery_shouldReturnNonNull() {
        when(auditReader.createQuery()).thenReturn(queryCreator);
        when(queryCreator.forRevisionsOfEntity(String.class, false, true)).thenReturn(auditQuery);
        when(auditQuery.addOrder(any())).thenReturn(auditQuery);
        when(auditQuery.setFirstResult(anyInt())).thenReturn(auditQuery);
        when(auditQuery.setMaxResults(anyInt())).thenReturn(auditQuery);
        when(auditQuery.add(any())).thenReturn(auditQuery);

        AuditQuery result = EnversUtils.buildFilteredAuditQuery(auditReader, String.class,1, new Date(), new Date(),0,10, "desc");
        assertNotNull(result);
    }

    @Test
    public void buildCountQueryWithFilters_shouldReturnNonNull() {
        when(auditReader.createQuery()).thenReturn(queryCreator);
        when(queryCreator.forRevisionsOfEntity(String.class, false, true)).thenReturn(auditQuery);
        when(auditQuery.addProjection(any())).thenReturn(auditQuery);
        when(auditQuery.add(any())).thenReturn(auditQuery);

        AuditQuery result = EnversUtils.buildCountQueryWithFilters(auditReader, String.class,1, new Date(), new Date());
        assertNotNull(result);
    }
}