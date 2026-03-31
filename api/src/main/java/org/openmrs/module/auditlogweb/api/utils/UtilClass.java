/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.auditlogweb.api.utils;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.envers.Audited;
import org.hibernate.proxy.HibernateProxy;
import org.openmrs.BaseOpenmrsObject;
import org.openmrs.module.auditlogweb.api.dto.AuditFieldDiff;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.MappedSuperclass;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Date;
import java.util.stream.Collectors;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Utility class providing methods for working with Envers-audited classes,
 * computing field-level differences, and date parsing/formatting for audit filtering.
 */
public class UtilClass {

    private static final Logger log = LoggerFactory.getLogger(UtilClass.class);

    private static List<String> classesWithAuditAnnotation;

    /**
     * Scans the {@code org.openmrs} package for all classes annotated with {@link Audited}
     * using Reflections and caches the result.
     *
     * @return a sorted list of fully qualified class names that are annotated with {@link Audited}
     */
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
                .filter(UtilClass::isConcreteAuditedEntity)
                .map(Class::getName)
                .sorted()
                .collect(Collectors.toList());

        return classesWithAuditAnnotation;
    }

    /**
     * Checks if a given class is annotated with {@link Audited}.
     *
     * @param clazz the class to inspect
     * @return {@code true} if the class is annotated with {@link Audited}, {@code false} otherwise
     */
    public static boolean doesClassContainsAuditedAnnotation(Class<?> clazz) {
        return clazz.isAnnotationPresent(Audited.class);
    }

    /**
     * Collects all fields from the entire class hierarchy (from Object to the given class).
     * In case of field name collisions, fields from child classes override fields from parent classes.
     * The fields are returned in order from parent to child.
     *
     * @param clazz the class to collect fields from
     * @return an array of fields from the entire hierarchy (child fields override parent fields)
     */
    private static Field[] getAllFields(Class<?> clazz) {
        Map<String, Field> fieldMap = new LinkedHashMap<>();
        
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            Field[] declaredFields = current.getDeclaredFields();
            for (Field field : declaredFields) {
                fieldMap.putIfAbsent(field.getName(), field);
            }
            current = current.getSuperclass();
        }
        
        return fieldMap.values().toArray(new Field[0]);
    }

    /**
     * Compares two instances of the same class and returns a list of field-level differences.
     * Fields that are static or synthetic are ignored. Values that cannot be accessed
     * are marked as "Unable to read".
     *
     * @param clazz         the class of the compared objects
     * @param oldEntity     the previous version of the object
     * @param currentEntity the current version of the object
     * @return a list of {@link AuditFieldDiff} showing name, old value, new value, and change flag
     */
    public static List<AuditFieldDiff> computeFieldDiffs(Class<?> clazz, Object oldEntity, Object currentEntity) {
        List<AuditFieldDiff> diffs = new ArrayList<>();
        if (currentEntity == null) return diffs;

        Field[] fields = getAllFields(clazz);
        for (Field field : fields) {
            if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) continue;

            field.setAccessible(true);

            String oldVal;
            String currVal;
            boolean failedOld = false;
            boolean failedCurr = false;

            try {
                Object currFieldValue = field.get(currentEntity);
                currVal = serializeFieldValue(currFieldValue);
            } catch (Exception e) {
                log.warn("Failed to read current value of field '{}': {}", field.getName(), e.getMessage());
                failedCurr = true;
                currVal = "";
            }

            try {
                Object oldFieldValue = oldEntity != null ? field.get(oldEntity) : null;
                oldVal = serializeFieldValue(oldFieldValue);
            } catch (Exception e) {
                log.warn("Failed to read old value of field '{}': {}", field.getName(), e.getMessage());
                failedOld = true;
                oldVal = "";
            }

            if (failedOld && failedCurr) {
                log.debug("Setting field '{}' values to empty string due to access failure", field.getName());
                oldVal = currVal = "";
            }

            boolean isDifferent = !Objects.equals(oldVal, currVal);
            diffs.add(new AuditFieldDiff(field.getName(), oldVal, currVal, isDifferent));
        }
        return diffs;
    }

    /**
     * Computes the total number of pages for a paginated dataset.
     *
     * @param total the total number of records
     * @param size  the number of records per page
     * @return the total number of pages
     */
    public static int computeTotalPages(long total, int size) {
        return (int) Math.ceil((double) total / size);
    }

    /**
     * Parses a date string into a {@link LocalDate}. Supports ISO format and "dd/MM/yyyy".
     *
     * @param dateStr the date string to parse
     * @return the parsed {@link LocalDate}, or {@code null} if parsing fails
     */
    public static LocalDate parse(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) return null;

        try {
            return LocalDate.parse(dateStr.trim());
        } catch (Exception e1) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                return LocalDate.parse(dateStr.trim(), formatter);
            } catch (Exception e2) {
                return null;
            }
        }
    }

    /**
     * Converts a {@link LocalDate} to a {@link Date} representing the start of the day.
     *
     * @param date the local date
     * @return the corresponding {@link Date} at 00:00, or {@code null} if input is null
     */
    public static Date toStartDate(LocalDate date) {
        return date == null ? null : Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    /**
     * Converts a {@link LocalDate} to a {@link Date} representing the end of the day (23:59:59.999).
     *
     * @param date the local date
     * @return the corresponding {@link Date} at end of day, or {@code null} if input is null
     */
    public static Date toEndDate(LocalDate date) {
        return date == null ? null : Date.from(date.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant());
    }

    /**
     * Paginates a given list in-memory.
     *
     * @param allResults the full list of items to paginate
     * @param page       the page number (0-based)
     * @param size       the number of items per page
     * @param <T>        the type of items in the list
     * @return a sublist representing the requested page
     */
    public static <T> List<T> paginate(List<T> allResults, int page, int size) {
        if (allResults == null || allResults.isEmpty()) {
            return Collections.emptyList();
        }

        int fromIndex = Math.min(page * size, allResults.size());
        int toIndex = Math.min(fromIndex + size, allResults.size());
        return allResults.subList(fromIndex, toIndex);
    }


    /**
     * Attempts to get the ID of the entity as a String.
     * Falls back to UUID or "unknown" if ID cannot be retrieved.
     *
     * @param entity the entity object
     * @return entity ID as String
     */
    public static String getEntityIdAsString(Object entity) {
        try {
            return String.valueOf(entity.getClass().getMethod("getId").invoke(entity));
        } catch (Exception e) {
            try {
                return String.valueOf(entity.getClass().getMethod("getUuid").invoke(entity));
            } catch (Exception ex) {
                return "unknown";
            }
        }
    }

    public static String serializeFieldValue(Object value) {
        if (value == null) {
            return "";
        }

        String actualClassName = getActualClassName(value);
        if (isPrimitiveOrWrapper(value.getClass())) {
            return String.valueOf(value);
        }

        if (value instanceof BaseOpenmrsObject) {
            return serializeBaseOpenmrsObject((BaseOpenmrsObject) value, actualClassName);
        }

        return serializeGenericObject(value, actualClassName);
    }

    public static String getActualClassName(Object obj) {
        if (obj instanceof HibernateProxy) {
            HibernateProxy proxy = (HibernateProxy) obj;
            return proxy.getHibernateLazyInitializer().getPersistentClass().getSimpleName();
        }

        return obj.getClass().getSimpleName();
    }

    private static boolean isConcreteAuditedEntity(Class<?> clazz) {
        int modifiers = clazz.getModifiers();
        return !Modifier.isAbstract(modifiers) && !clazz.isAnnotationPresent(MappedSuperclass.class);
    }

    private static boolean isPrimitiveOrWrapper(Class<?> clazz) {
        return clazz.isPrimitive()
                || clazz == Boolean.class
                || clazz == Character.class
                || clazz == Byte.class
                || clazz == Short.class
                || clazz == Integer.class
                || clazz == Long.class
                || clazz == Float.class
                || clazz == Double.class
                || clazz == String.class
                || clazz == Date.class
                || Number.class.isAssignableFrom(clazz)
                || Temporal.class.isAssignableFrom(clazz);
    }

    private static String serializeBaseOpenmrsObject(BaseOpenmrsObject obj, String actualClassName) {
        Integer id = getIdFromObject(obj);
        if (id != null) {
            return actualClassName + "#" + id;
        }

        String uuid = getUuidFromObject(obj);
        if (StringUtils.isNotBlank(uuid)) {
            return actualClassName + "#" + uuid;
        }

        return "";
    }

    private static Integer getIdFromObject(Object obj) {
        try {
            Method method = obj.getClass().getMethod("getId");
            Object result = method.invoke(obj);
            if (result instanceof Integer) {
                return (Integer) result;
            }
        } catch (Exception e) {
            log.debug("Failed to get ID from object: {}", e.getMessage());
        }

        return null;
    }

    private static String getUuidFromObject(Object obj) {
        try {
            Method method = obj.getClass().getMethod("getUuid");
            Object result = method.invoke(obj);
            if (result instanceof String) {
                return (String) result;
            }
        } catch (Exception e) {
            log.debug("Failed to get UUID from object: {}", e.getMessage());
        }

        return null;
    }

    private static String serializeGenericObject(Object value, String actualClassName) {
        Integer id = getIdFromObject(value);
        if (id != null) {
            return actualClassName + "#" + id;
        }

        String uuid = getUuidFromObject(value);
        if (StringUtils.isNotBlank(uuid)) {
            return actualClassName + "#" + uuid;
        }

        try {
            String str = String.valueOf(value);
            if (StringUtils.isNotBlank(str)) {
                if (StringUtils.equalsIgnoreCase(str, "[]")) {
                    str = "";
                }
                return str;
            }
        } catch (Exception e) {
            log.debug("Failed to serialize object with toString(): {}", e.getMessage());
        }

        return "";
    }

    public static Map<String, Class<?>> getFieldTypes(Class<?> clazz) {
        Map<String, Class<?>> fieldTypes = new LinkedHashMap<>();
        Field[] fields = getAllFields(clazz);
        
        for (Field field : fields) {
            if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
                continue;
            }
            
            Class<?> fieldType = field.getType();
            if (Collection.class.isAssignableFrom(fieldType)) {
                ParameterizedType parameterizedType = (ParameterizedType) field.getGenericType();
                if (parameterizedType.getActualTypeArguments().length > 0) {
                    Type actualType = parameterizedType.getActualTypeArguments()[0];
                    if (actualType instanceof Class) {
                        fieldTypes.put(field.getName(), (Class<?>) actualType);
                    }
                }
            } else {
                fieldTypes.put(field.getName(), fieldType);
            }
        }
        
        return fieldTypes;
    }
}