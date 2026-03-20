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
import org.junit.Test;
import org.openmrs.module.auditlogweb.api.dto.AuditFieldDiff;
import java.time.LocalDate;
import java.time.Month;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;


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

    @Test
    public void computeFieldDiffs_shouldIncludeFieldsFromParentClass() {
        ParentClass parent = new ParentClass();
        parent.setParentField("parentValue");

        ChildClass child = new ChildClass();
        child.setParentField("parentValue");
        child.setChildField("childValue");

        List<AuditFieldDiff> diffs = UtilClass.computeFieldDiffs(ChildClass.class, parent, child);

        boolean foundParentField = false;
        boolean foundChildField = false;
        
        for (AuditFieldDiff diff : diffs) {
            if ("parentField".equals(diff.getFieldName())) {
                foundParentField = true;
            }
            if ("childField".equals(diff.getFieldName())) {
                foundChildField = true;
            }
        }
        
        assertTrue(foundParentField);
        assertTrue(foundChildField);
    }

    @Test
    public void computeFieldDiffs_shouldDetectChangesInInheritedFields() {
        PersonImpl oldPerson = new PersonImpl();
        oldPerson.setGender("M");
        oldPerson.setBirthdate(new Date());
        oldPerson.setPersonId(1);

        PersonImpl newPerson = new PersonImpl();
        newPerson.setGender("F");
        newPerson.setBirthdate(new Date());
        newPerson.setPersonId(1);

        List<AuditFieldDiff> diffs = UtilClass.computeFieldDiffs(PersonImpl.class, oldPerson, newPerson);

        AuditFieldDiff genderDiff = null;
        for (AuditFieldDiff diff : diffs) {
            if ("gender".equals(diff.getFieldName())) {
                genderDiff = diff;
                break;
            }
        }
        
        assertNotNull(genderDiff);
        assertEquals("M", genderDiff.getOldValue());
        assertEquals("F", genderDiff.getCurrentValue());
        assertTrue(genderDiff.isChanged());
    }

    @Test
    public void computeFieldDiffs_shouldHandleNullOldEntity() {
        ChildClass child = new ChildClass();
        child.setChildField("value");

        List<AuditFieldDiff> diffs = UtilClass.computeFieldDiffs(ChildClass.class, null, child);
        
        boolean foundChildField = false;
        for (AuditFieldDiff diff : diffs) {
            if ("childField".equals(diff.getFieldName())) {
                foundChildField = true;
                break;
            }
        }
        assertTrue(foundChildField);
    }

    // Dummy Audited class for testing only
    @Audited
    public static class TestAuditedClass {}

    public static class NotAuditedClass {}

    public static class ParentClass {
        private String parentField;
        private String commonField;

        public String getParentField() { return parentField; }
        public void setParentField(String parentField) { this.parentField = parentField; }
        public String getCommonField() { return commonField; }
        public void setCommonField(String commonField) { this.commonField = commonField; }
    }

    public static class ChildClass extends ParentClass {
        private String childField;
        private String commonField;

        public String getChildField() { return childField; }
        public void setChildField(String childField) { this.childField = childField; }
        @Override
        public String getCommonField() { return commonField; }
        @Override
        public void setCommonField(String commonField) { this.commonField = commonField; }
    }

    public static class PersonImpl {
        private Integer personId;
        private String gender;
        private Date birthdate;

        public Integer getPersonId() { return personId; }
        public void setPersonId(Integer personId) { this.personId = personId; }
        public String getGender() { return gender; }
        public void setGender(String gender) { this.gender = gender; }
        public Date getBirthdate() { return birthdate; }
        public void setBirthdate(Date birthdate) { this.birthdate = birthdate; }
    }
}
