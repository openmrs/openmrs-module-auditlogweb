package org.openmrs.module.auditlogweb.api.utils;

import static org.junit.jupiter.api.Assertions.assertTrue;
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

        // Assert that all found audited classes are within the correct package
        for (String clazz : actualAuditedClasses) {
            assertTrue(clazz.startsWith("org.openmr"), "Audited class outside expected package: " + clazz);
        }

        // Assert that expected audited classes are among those found
        for (String expected : expectedClassNames) {
            assertTrue(actualAuditedClasses.contains(expected), "Expected audited class not found: " + expected);
        }
    }
}