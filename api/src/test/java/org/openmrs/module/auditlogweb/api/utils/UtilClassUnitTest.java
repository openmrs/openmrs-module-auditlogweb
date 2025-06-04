package org.openmrs.module.auditlogweb.api.utils;

import org.hibernate.envers.Audited;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.util.List;
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
        assertTrue(!UtilClass.doesClassContainsAuditedAnnotation(NotAuditedClass.class));
    }

    // Dummy Audited class for testing only
    @Audited
    public static class TestAuditedClass {}

    public static class NotAuditedClass {}
}
