/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.auditlogweb.api.utils;

import org.hibernate.envers.Audited;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.openmrs.Concept;
import org.openmrs.EncounterType;
import org.openmrs.api.ConceptService;
import org.openmrs.api.context.Context;
import org.openmrs.module.auditlogweb.api.dto.AuditFieldDiff;
import java.time.LocalDate;
import java.time.Month;
import java.util.Date;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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
		LocalDate date=LocalDate.of(2025,Month.JULY,9);Date endDate=UtilClass.toEndDate(date);assertNotNull(endDate);
		// Expecting 23:59:59.999 (999 milliseconds = 999_000_000 nanos)
		assertEquals(date.atTime(23,59,59).plusNanos(999_000_000).atZone(java.time.ZoneId.systemDefault()).toInstant(),endDate.toInstant());assertNull(UtilClass.toEndDate(null));
	}
	
	@Test
	public void parseDate_shouldReturnCorrectDate() {
		LocalDate date=LocalDate.of(2025,Month.JULY,9);Date startDate=UtilClass.parseDate("09/07/2025",false);assertNotNull(startDate);assertEquals(date.atStartOfDay().atZone(java.time.ZoneId.systemDefault()).toInstant(),startDate.toInstant());
		
		Date endDate=UtilClass.parseDate("09/07/2025",true);assertNotNull(endDate);assertEquals(date.atTime(23,59,59).plusNanos(999_000_000).atZone(java.time.ZoneId.systemDefault()).toInstant(),endDate.toInstant());
	}
	
	@Test
	public void parseDate_shouldReturnNullForNullOrEmpty() {
		assertNull(UtilClass.parseDate(null, false));
		assertNull(UtilClass.parseDate("", false));
		assertNull(UtilClass.parseDate(" ", false));
	}
	
	@Test
	public void parseDate_shouldThrowIllegalArgumentExceptionForInvalidDate() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			UtilClass.parseDate("2025/07/09", false);
		});
		assertTrue(exception.getMessage().contains("Invalid month date or date format"));
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
	
	@Test
	public void serializeFieldValue_shouldRenderSqlDateAsLocalDateOnly() {
		assertEquals("2000-01-15", UtilClass.serializeFieldValue(java.sql.Date.valueOf("2000-01-15")));
	}
	
	@Test
	public void serializeFieldValue_shouldRenderUtilDateInSystemZone() {
		java.util.TimeZone original = java.util.TimeZone.getDefault();
		try {
			java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("UTC"));
			assertEquals("1970-01-01T00:00:00Z", UtilClass.serializeFieldValue(new Date(0L)));
			
			// Non-UTC: a date-only value at local midnight must keep its local calendar day, not shift a day.
			java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Asia/Kolkata"));
			java.util.Calendar cal = java.util.Calendar.getInstance();
			cal.clear();
			cal.set(2000, java.util.Calendar.JANUARY, 15, 0, 0, 0);
			assertEquals("2000-01-15T00:00:00+05:30", UtilClass.serializeFieldValue(new Date(cal.getTimeInMillis())));
		}
		finally {
			java.util.TimeZone.setDefault(original);
		}
	}
	
	@Test
	public void resolveDisplayValue_shouldReturnNullForNonEntityValues() {
		assertNull(UtilClass.resolveDisplayValue("just a string"));
		assertNull(UtilClass.resolveDisplayValue(42));
		assertNull(UtilClass.resolveDisplayValue(null));
	}
	
	@Test
	public void resolveDisplayValue_shouldResolveConceptDisplayLiveById() {
		Concept reference = new Concept();
		reference.setConceptId(88);
		
		Concept live = mock(Concept.class);
		when(live.getDisplayString()).thenReturn("Malaria");
		ConceptService conceptService = mock(ConceptService.class);
		when(conceptService.getConcept(88)).thenReturn(live);
		
		try (MockedStatic<Context> context = mockStatic(Context.class)) {
			context.when(Context::getConceptService).thenReturn(conceptService);
			assertEquals("Malaria (Concept#88)", UtilClass.resolveDisplayValue(reference));
		}
	}
	
	@Test
	public void resolveDisplayValue_shouldResolveMetadataByName() {
		EncounterType encounterType = new EncounterType();
		encounterType.setEncounterTypeId(7);
		encounterType.setName("Vitals");
		assertEquals("Vitals (EncounterType#7)", UtilClass.resolveDisplayValue(encounterType));
	}
	
	@Test
	public void resolveDisplayValue_shouldMemoizeLiveLookupsWithinACache() {
		Concept reference = new Concept();
		reference.setConceptId(88);
		
		Concept live = mock(Concept.class);
		when(live.getDisplayString()).thenReturn("Malaria");
		ConceptService conceptService = mock(ConceptService.class);
		when(conceptService.getConcept(88)).thenReturn(live);
		Map<String, String> cache = new HashMap<>();
		
		try (MockedStatic<Context> context = mockStatic(Context.class)) {
			context.when(Context::getConceptService).thenReturn(conceptService);
			assertEquals("Malaria (Concept#88)", UtilClass.resolveDisplayValue(reference, cache));
			assertEquals("Malaria (Concept#88)", UtilClass.resolveDisplayValue(reference, cache));
			verify(conceptService, times(1)).getConcept(88);
		}
	}
	
	@Test
	void testSanitizePageSize_Normal() {
		assertEquals(50, UtilClass.sanitizePageSizeValue(50));
	}
	
	@Test
	void testSanitizePageSize_ZeroOrNegative() {
		assertEquals(15, UtilClass.sanitizePageSizeValue(0));
		assertEquals(15, UtilClass.sanitizePageSizeValue(-5));
	}
	
	@Test
	void testSanitizePageSize_ExceedsMax() {
		int expectedMax = AuditLogConstants.MAX_PAGE_SIZE;
		assertEquals(expectedMax, UtilClass.sanitizePageSizeValue(9999));
	}
	
	@Test
	void testSanitizePage_Normal() {
		assertEquals(2, UtilClass.sanitizePageValue(2, 20));
	}
	
	@Test
	void testSanitizePage_Negative() {
		assertEquals(0, UtilClass.sanitizePageValue(-1, 20));
	}
	
	@Test
	void testSanitizePage_OverflowPrevention() {
		int size = 100;
		int maxSafePage = Integer.MAX_VALUE / size;
		
		int sanitizedPage = UtilClass.sanitizePageValue(Integer.MAX_VALUE, size);
		
		assertEquals(maxSafePage, sanitizedPage);
	}
	
	@Test
	void testSanitizePage_WithInvalidSize() {
		int sanitizedPage = UtilClass.sanitizePageValue(5, 0);
		
		assertEquals(5, sanitizedPage);
	}
	
	// Dummy Audited class for testing only
	@Audited
	public static class TestAuditedClass {}
	
	public static class NotAuditedClass {}
	
	public static class ParentClass {
		
		private String parentField;
		
		private String commonField;
		
		public String getParentField() {
			return parentField;
		}
		
		public void setParentField(String parentField) {
			this.parentField = parentField;
		}
		
		public String getCommonField() {
			return commonField;
		}
		
		public void setCommonField(String commonField) {
			this.commonField = commonField;
		}
	}
	
	public static class ChildClass extends ParentClass {
		
		private String childField;
		
		private String commonField;
		
		public String getChildField() {
			return childField;
		}
		
		public void setChildField(String childField) {
			this.childField = childField;
		}
		
		@Override
		public String getCommonField() {
			return commonField;
		}
		
		@Override
		public void setCommonField(String commonField) {
			this.commonField = commonField;
		}
	}
	
	public static class PersonImpl extends BaseOpenmrsData {
		
		private Integer personId;
		
		private String gender;
		
		private Date birthdate;
		
		public Integer getPersonId() {
			return personId;
		}
		
		public void setPersonId(Integer personId) {
			this.personId = personId;
		}
		
		public String getGender() {
			return gender;
		}
		
		public void setGender(String gender) {
			this.gender = gender;
		}
		
		public Date getBirthdate() {
			return birthdate;
		}
		
		public void setBirthdate(Date birthdate) {
			this.birthdate = birthdate;
		}
	}
	
	public static class BaseOpenmrsData extends BaseOpenmrsObject {}
	
	public static class BaseOpenmrsObject {
		
		private String uuid;
		
		public String getUuid() {
			return uuid;
		}
		
		public void setUuid(String uuid) {
			this.uuid = uuid;
		}
	}
}
