/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.auditlogweb.api.utils;

import org.hibernate.envers.Audited;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.time.Month;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;


public class UtilClassUnitTest {
    @Test
    public void findClassesWithAuditedAnnotation() {
        List<String> auditedClasses = UtilClass.findClassesWithAnnotation();
        assertTrue(auditedClasses.contains(TestAuditedClass.class.getName()));
    }

    @Test
    public void doesClassContainsAuditedAnnotation() {
        assertTrue(UtilClass.doesClassContainsAuditedAnnotation(TestAuditedClass.class));
        assertFalse(UtilClass.doesClassContainsAuditedAnnotation(NotAuditedClass.class));
    }

    @Test
    public void parse_shouldReturnCorrectLocalDateOrNull() {
        // ISO format
        assertEquals(LocalDate.of(2025, 7, 9), UtilClass.parse("2025-07-09"));
        assertEquals(LocalDate.of(2025, 7, 9), UtilClass.parse("09/07/2025"));
        assertNull(UtilClass.parse(null));
        assertNull(UtilClass.parse(""));

        assertNull(UtilClass.parse("invalid-date"));
    }

    @Test
    public void toStartDate_shouldReturnDateAtStartOfDayOrNull() {
        LocalDate date = LocalDate.of(2025, Month.JULY, 9);
        Date startDate = UtilClass.toStartDate(date);
        assertNotNull(startDate);
        assertEquals(date.atStartOfDay().atZone(java.time.ZoneId.systemDefault()).toInstant(), startDate.toInstant());
        assertNull(UtilClass.toStartDate(null));
    }

    @Test
    public void toEndDate_shouldReturnDateAtEndOfDayOrNull() {
        LocalDate date = LocalDate.of(2025, Month.JULY, 9);
        Date endDate = UtilClass.toEndDate(date);
        assertNotNull(endDate);
        // Expecting 23:59:59.999 (999 milliseconds = 999_000_000 nanos)
        assertEquals(date.atTime(23, 59, 59).plusNanos(999_000_000).atZone(java.time.ZoneId.systemDefault()).toInstant(), endDate.toInstant());
        assertNull(UtilClass.toEndDate(null));
    }

    // Dummy Audited class for testing only
    @Audited
    public static class TestAuditedClass {}

    public static class NotAuditedClass {}
}
