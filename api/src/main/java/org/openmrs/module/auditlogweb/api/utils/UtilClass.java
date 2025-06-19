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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class UtilClass {
    private static final Logger log = LoggerFactory.getLogger(UtilClass.class);
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

        if (currentEntity == null) return diffs;

        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) continue;

            field.setAccessible(true);

            String oldVal = null;
            String currVal = null;
            boolean failedOld = false;
            boolean failedCurr = false;

            try {
                currVal = currentEntity != null ? String.valueOf(field.get(currentEntity)) : null;
            } catch (Exception e) {
                log.warn("Failed to read current value of field '{}': {}", field.getName(), e.getMessage());
                failedCurr = true;
            }

            try {
                oldVal = oldEntity != null ? String.valueOf(field.get(oldEntity)) : null;
            } catch (Exception e) {
                log.warn("Failed to read old value of field '{}': {}", field.getName(), e.getMessage());
                failedOld = true;
            }

            if (failedOld || failedCurr) {
                log.debug("Setting field '{}' values to 'Unable to read' due to access failure", field.getName());
                oldVal = currVal = "Unable to read";
            }

            boolean isDifferent = !Objects.equals(oldVal, currVal);
            diffs.add(new AuditFieldDiff(field.getName(), oldVal, currVal, isDifferent));
        }
        return diffs;
    }
}
