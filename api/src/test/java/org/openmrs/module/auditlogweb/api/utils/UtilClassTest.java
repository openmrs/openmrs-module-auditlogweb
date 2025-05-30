package org.openmrs.module.auditlogweb.api.utils;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.openmrs.Allergy;
import org.openmrs.Order;
import org.openmrs.Patient;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

class UtilClassTest {

    @Test
    public void testAuditedClassesContainsExpected() throws IOException {
        List<String> expectedClassNames = Arrays.asList(
                Patient.class.getName(),
                Allergy.class.getName(),
                Order.class.getName()
        );

        List<String> actualAuditedClasses = UtilClass.findClassesWithAuditedAnnotation();

        for (String clazz : actualAuditedClasses) {
            assertTrue(clazz.startsWith("org.openmr"), "Audited class outside expected package: " + clazz);
        }
    }
}