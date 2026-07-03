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

import org.hibernate.envers.AuditTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openmrs.module.auditlogweb.api.dao.AuditBackfillDao.TableMapping;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AuditBackfillDaoTest {
	
	private AuditBackfillDao dao;
	
	@BeforeEach
	void setUp() {
		dao = new AuditBackfillDao(null);
	}
	
	@Test
	void shouldBuildInsertSqlWithRevtypeAndBacktickQuoting() {
		String sql = dao.buildBackfillInsertSql(new TableMapping("patient", "patient_aud"),
		    Arrays.asList("patient_id", "name"), Collections.singletonList("patient_id"), true, 7, "`");
		
		assertEquals("INSERT INTO `patient_aud` (`patient_id`, `name`, REV, REVTYPE) SELECT "
		        + "b.`patient_id`, b.`name`, 7, 0 FROM `patient` b WHERE NOT EXISTS "
		        + "(SELECT 1 FROM `patient_aud` a WHERE a.`patient_id` = b.`patient_id`)",
		    sql);
	}
	
	@Test
	void shouldBuildInsertSqlWithoutRevtypeAndNoQuoting() {
		String sql = dao.buildBackfillInsertSql(new TableMapping("obs_reference_range", "obs_reference_range_aud"),
		    Collections.singletonList("obs_id"), Collections.singletonList("obs_id"), false, 3, "");
		
		assertEquals("INSERT INTO obs_reference_range_aud (obs_id, REV) SELECT b.obs_id, 3 FROM obs_reference_range b "
		        + "WHERE NOT EXISTS (SELECT 1 FROM obs_reference_range_aud a WHERE a.obs_id = b.obs_id)",
		    sql);
	}
	
	@Test
	void shouldOrderParentAuditTableBeforeChild() {
		TableMapping child = new TableMapping("patient", "patient_aud");
		TableMapping parent = new TableMapping("person", "person_aud");
		Map<String, Set<String>> parents = new HashMap<>();
		parents.put("patient_aud", singleton("person_aud"));
		parents.put("person_aud", emptySet());
		
		List<TableMapping> ordered = dao.orderParentsBeforeChildren(Arrays.asList(child, parent), parents);
		
		assertEquals(Arrays.asList("person_aud", "patient_aud"), auditNames(ordered));
	}
	
	@Test
	void shouldPreserveOrderWhenNoDependencies() {
		TableMapping a = new TableMapping("a", "a_aud");
		TableMapping b = new TableMapping("b", "b_aud");
		Map<String, Set<String>> parents = new HashMap<>();
		parents.put("a_aud", emptySet());
		parents.put("b_aud", emptySet());
		
		List<TableMapping> ordered = dao.orderParentsBeforeChildren(Arrays.asList(a, b), parents);
		
		assertEquals(Arrays.asList("a_aud", "b_aud"), auditNames(ordered));
	}
	
	@Test
	void shouldFallBackToOriginalOrderOnDependencyCycle() {
		TableMapping a = new TableMapping("a", "a_aud");
		TableMapping b = new TableMapping("b", "b_aud");
		Map<String, Set<String>> parents = new HashMap<>();
		parents.put("a_aud", singleton("b_aud"));
		parents.put("b_aud", singleton("a_aud"));
		
		List<TableMapping> ordered = dao.orderParentsBeforeChildren(Arrays.asList(a, b), parents);
		
		assertEquals(Arrays.asList("a_aud", "b_aud"), auditNames(ordered));
	}
	
	@Test
	void shouldDeriveAuditTableNameFromPrefixAndSuffixWhenNoAnnotation() {
		assertEquals("aud_person_hist", dao.deriveAuditTableName(Plain.class, "person", "aud_", "_hist"));
		assertEquals("orders_audit", dao.deriveAuditTableName(Plain.class, "orders", "", "_audit"));
	}
	
	@Test
	void shouldDeriveAuditTableNameFromAuditTableAnnotation() {
		assertEquals("patient_appointment_revisions",
		    dao.deriveAuditTableName(Annotated.class, "patient_appointment", "", "_audit"));
	}
	
	@Test
	void shouldQuoteIdentifierAndEscapeEmbeddedQuoteCharacter() {
		assertEquals("`patient`", dao.quoteIdentifier("patient", "`"));
		assertEquals("patient", dao.quoteIdentifier("patient", ""));
		assertEquals("`we``ird`", dao.quoteIdentifier("we`ird", "`"));
		assertEquals("\"we\"\"ird\"", dao.quoteIdentifier("we\"ird", "\""));
	}
	
	private static List<String> auditNames(List<TableMapping> mappings) {
		return mappings.stream().map(TableMapping::getAuditTable).collect(java.util.stream.Collectors.toList());
	}
	
	private static class Plain {}
	
	@AuditTable("patient_appointment_revisions")
	private static class Annotated {}
	
}
