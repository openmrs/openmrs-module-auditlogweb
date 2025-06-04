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
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UtilClassUnitTest {
    @Test
    public void findClassesWithAuditedAnnotation() {
        List<String> auditedClasses = UtilClass.findClassesWithAnnotation();

        //confirming that TestAuditedClass is picked up correctly
        assertTrue(auditedClasses.contains(TestAuditedClass.class.getName()));
    }

    @Test
    public void doesClassContainsAuditedAnnotation() {
        assertTrue(UtilClass.doesClassContainsAuditedAnnotation(TestAuditedClass.class));
        assertFalse(UtilClass.doesClassContainsAuditedAnnotation(NotAuditedClass.class));
    }

    // Dummy Audited class for testing only
    @Audited
    public static class TestAuditedClass {}

    public static class NotAuditedClass {}
}
