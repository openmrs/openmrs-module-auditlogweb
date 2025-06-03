package org.openmrs.module.auditlogweb.api.utils;

import org.hibernate.envers.Audited;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UtilClassUnitTest {
    private MetadataReader metadataReader;
    private MetadataReaderFactory metadataReaderFactory;
    private Resource resource;

    @BeforeEach
    public void setUp() throws IOException {
        metadataReader = mock(MetadataReader.class);
        metadataReaderFactory = mock(MetadataReaderFactory.class);
        resource = mock(Resource.class);

        // Mockinf of ClassMetadata and its behavior
        org.springframework.core.type.ClassMetadata classMetadata = mock(org.springframework.core.type.ClassMetadata.class);
        when(classMetadata.getClassName()).thenReturn(TestAuditedClass.class.getName());
        when(metadataReader.getClassMetadata()).thenReturn(classMetadata);

        when(resource.isReadable()).thenReturn(true);
        when(metadataReaderFactory.getMetadataReader(resource)).thenReturn(metadataReader);
    }

    @Test
    public void testClassWithAuditedAnnotationIsDetected() throws Exception {
        class TestUtilClass extends UtilClass {
            public List<String> test(List<Resource> resources, MetadataReaderFactory metadataReaderFactory) throws IOException {
                List<String> result = new ArrayList<>();
                for (Resource resource : resources) {
                    if (resource.isReadable()) {
                        MetadataReader reader = metadataReaderFactory.getMetadataReader(resource);
                        if (UtilClass.doesClassContainsAuditedAnnotation(reader)) {
                            result.add(reader.getClassMetadata().getClassName());
                        }
                    }
                }
                return result;
            }
        }

        TestUtilClass util = new TestUtilClass();
        List<String> foundClasses = util.test(Arrays.asList(resource), metadataReaderFactory);

        assertEquals(1, foundClasses.size());
        assertEquals(TestAuditedClass.class.getName(), foundClasses.get(0));
    }
    @Test
    public void testClassWithoutAuditedAnnotationIsNotDetected() throws Exception {
        class NotAudited {}

        org.springframework.core.type.ClassMetadata classMetadata = mock(org.springframework.core.type.ClassMetadata.class);
        when(classMetadata.getClassName()).thenReturn(NotAudited.class.getName());
        when(metadataReader.getClassMetadata()).thenReturn(classMetadata);
        when(metadataReaderFactory.getMetadataReader(resource)).thenReturn(metadataReader);

        class TestUtilClass extends UtilClass {
            public List<String> test(List<Resource> resources, MetadataReaderFactory metadataReaderFactory) throws IOException {
                List<String> result = new ArrayList<>();
                for (Resource resource : resources) {
                    if (resource.isReadable()) {
                        MetadataReader reader = metadataReaderFactory.getMetadataReader(resource);
                        if (UtilClass.doesClassContainsAuditedAnnotation(reader)) {
                            result.add(reader.getClassMetadata().getClassName());
                        }
                    }
                }
                return result;
            }
        }

        TestUtilClass util = new TestUtilClass();
        List<String> foundClasses = util.test(Arrays.asList(resource), metadataReaderFactory);

        assertEquals(0, foundClasses.size());
    }


    // Dummy Audited class for testing only
    @Audited
    public static class TestAuditedClass {}
}
