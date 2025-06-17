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
import org.openmrs.module.auditlogweb.api.dto.AuditFieldDiff;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class UtilClass {
    private static List<String> classesWithAuditAnnotation;

    public static List<String> findClassesWithAnnotation() {
        if (classesWithAuditAnnotation != null) {
            return classesWithAuditAnnotation;
        }

        Reflections reflections = new Reflections(new ConfigurationBuilder().setUrls(ClasspathHelper.forPackage("org.openmrs")).setScanners(Scanners.TypesAnnotated));

        Set<Class<?>> auditedClasses = reflections.getTypesAnnotatedWith(Audited.class);
        classesWithAuditAnnotation = auditedClasses.stream().map(Class::getName).sorted().collect(Collectors.toList());
        return classesWithAuditAnnotation;
    }

    public static boolean doesClassContainsAuditedAnnotation(Class<?> clazz) {
        return clazz.isAnnotationPresent(Audited.class);
    }

    public static List<AuditFieldDiff> computeFieldDiffs(Class<?> clazz, Object oldEntity, Object currentEntity) {
        List<AuditFieldDiff> diffs = new ArrayList<>();

        if (currentEntity == null) {
            return diffs;
        }
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
                continue;
            }
            field.setAccessible(true);
            String oldString = readFieldValueSafely(field, oldEntity);
            String currentString = readFieldValueSafely(field, currentEntity);
            boolean isDifferent = !Objects.equals(oldString, currentString);

            diffs.add(new AuditFieldDiff(field.getName(), oldString, currentString, isDifferent));
        }
        return diffs;
    }

    public static String readFieldValueSafely(Field field, Object entity) {
        if (entity == null) return null;
        try {
            Object value = field.get(entity);
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            return "Unable to read";
        }

    }
}
