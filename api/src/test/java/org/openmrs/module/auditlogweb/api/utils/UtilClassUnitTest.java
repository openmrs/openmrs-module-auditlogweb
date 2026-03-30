/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.auditlogweb.api.utils;

import java.time.LocalDate;
import java.time.Month;
import java.util.Date;
import java.util.List;

import org.hibernate.envers.Audited;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;


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
    
    @Test
    public void computeFieldDiffs_shouldReturnNoValueForNullFields() {
        SampleEntity old = new SampleEntity(null, "old@email.com");
        SampleEntity current = new SampleEntity(null, "new@email.com");

        List<org.openmrs.module.auditlogweb.api.dto.AuditFieldDiff> diffs =
                UtilClass.computeFieldDiffs(SampleEntity.class, old, current);

        // null field should display as "[No value]" not the string "null"
        diffs.stream()
                .filter(d -> d.getFieldName().equals("name"))
                .forEach(d -> {
                    assertEquals("[No value]", d.getOldValue());
                    assertEquals("[No value]", d.getCurrentValue());
                });
    }

    @Test
    public void computeFieldDiffs_shouldDetectChangedFields() {
        SampleEntity old = new SampleEntity("Alice", "alice@email.com");
        SampleEntity current = new SampleEntity("Bob", "alice@email.com");

        List<org.openmrs.module.auditlogweb.api.dto.AuditFieldDiff> diffs =
                UtilClass.computeFieldDiffs(SampleEntity.class, old, current);

        org.openmrs.module.auditlogweb.api.dto.AuditFieldDiff nameDiff = diffs.stream()
                .filter(d -> d.getFieldName().equals("name"))
                .findFirst().orElse(null);

        assertNotNull(nameDiff);
        assertTrue(nameDiff.isChanged());
        assertEquals("Alice", nameDiff.getOldValue());
        assertEquals("Bob", nameDiff.getCurrentValue());
    }

    @Test
    public void computeFieldDiffs_shouldNotMarkUnchangedFieldsAsChanged() {
        SampleEntity old = new SampleEntity("Alice", "alice@email.com");
        SampleEntity current = new SampleEntity("Alice", "alice@email.com");

        List<org.openmrs.module.auditlogweb.api.dto.AuditFieldDiff> diffs =
                UtilClass.computeFieldDiffs(SampleEntity.class, old, current);

        diffs.forEach(d -> assertFalse(d.isChanged()));
    }

    @Test
    public void computeFieldDiffs_shouldHandleNullOldEntity() {
        SampleEntity current = new SampleEntity("Alice", "alice@email.com");

        List<org.openmrs.module.auditlogweb.api.dto.AuditFieldDiff> diffs =
                UtilClass.computeFieldDiffs(SampleEntity.class, null, current);

        // All fields should be marked changed (old is null, current has values)
        assertFalse(diffs.isEmpty());
        diffs.forEach(d -> assertNull(d.getOldValue()) );
    }

    @Test
    public void computeFieldDiffs_shouldReturnEmptyListForNullCurrentEntity() {
        List<org.openmrs.module.auditlogweb.api.dto.AuditFieldDiff> diffs =
                UtilClass.computeFieldDiffs(SampleEntity.class, null, null);

        assertTrue(diffs.isEmpty());
    }

    // Simple test entity — no Hibernate, no OpenMRS dependencies
    public static class SampleEntity {
        private String name;
        private String email;

        public SampleEntity(String name, String email) {
            this.name = name;
            this.email = email;
        }
    }
}
