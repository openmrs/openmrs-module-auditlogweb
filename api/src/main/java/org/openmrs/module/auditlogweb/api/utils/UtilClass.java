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
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.util.ClassUtils;
import org.springframework.util.SystemPropertyUtils;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.Collectors;

public class UtilClass {
    private static List<String> classesWithAuditAnnotation;

    public static List<String> findClassesWithAuditedAnnotation() throws IOException {
        if (classesWithAuditAnnotation != null) {
            return classesWithAuditAnnotation;
        }

        ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
        MetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory(resourcePatternResolver);

        //Search only for Service Classes in DWR package.
        String packageSearchPath = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX + resolveBasePackage() + "/**/*.class";

        List<String> candidateClasses = new ArrayList<>();
        Resource[] resources = resourcePatternResolver.getResources(packageSearchPath);
        for (Resource resource : resources) {
            if (resource.isReadable()) {
                MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(resource);
                if (doesClassContainsAuditedAnnotation(metadataReader)) {
                    candidateClasses.add(metadataReader.getClassMetadata().getClassName());
                }
            }
        }

        classesWithAuditAnnotation = candidateClasses.stream()
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());
        return classesWithAuditAnnotation;
    }

    private static String resolveBasePackage() {
        return ClassUtils.convertClassNameToResourcePath(SystemPropertyUtils.resolvePlaceholders("org.openmrs"));
    }

    public static boolean doesClassContainsAuditedAnnotation(MetadataReader metadataReader) {
        try {
            Class<?> dwrClass = Class.forName(metadataReader.getClassMetadata().getClassName());

            if (dwrClass.isAnnotationPresent(Audited.class)) {
                return true;
            }
        } catch (Throwable ignored) {}
        return false;
    }
}
