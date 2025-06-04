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
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class UtilClass {
    private static List<String> classesWithAuditAnnotation;

    public static List<String> findClassesWithAnnotation() {
        if (classesWithAuditAnnotation != null) {
            return classesWithAuditAnnotation;
        }

        Reflections reflections = new Reflections(
                new ConfigurationBuilder()
                        .setUrls(ClasspathHelper.forPackage("org.openmrs"))
                        .setScanners(Scanners.TypesAnnotated)
        );

        Set<Class<?>> auditedClasses = reflections.getTypesAnnotatedWith(Audited.class);
        classesWithAuditAnnotation = auditedClasses.stream()
                .map(Class::getName)
                .sorted()
                .collect(Collectors.toList());
        return classesWithAuditAnnotation;
    }
    public static boolean doesClassContainsAuditedAnnotation(Class<?> clazz) {
        return clazz.isAnnotationPresent(Audited.class);
    }
}
