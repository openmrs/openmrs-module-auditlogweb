/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.auditlogweb.api.dao;

import lombok.RequiredArgsConstructor;
import org.hibernate.SessionFactory;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.exception.NotAuditedException;
import org.hibernate.envers.query.AuditQuery;
import org.hibernate.exception.SQLGrammarException;
import org.openmrs.api.db.hibernate.envers.OpenmrsRevisionEntity;
import org.openmrs.module.auditlogweb.AuditEntity;
import org.openmrs.module.auditlogweb.api.utils.EnversUtils;
import org.openmrs.module.auditlogweb.api.utils.UtilClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import org.openmrs.GlobalProperty;
import org.openmrs.Role;

import java.lang.reflect.Modifier;
import java.sql.SQLSyntaxErrorException;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.util.stream.Collectors;

/**
 * Data access object (DAO) for retrieving audit log information using Hibernate Envers.
 * This DAO provides methods for fetching entity revisions and revision metadata such as
 * who made the change, what was changed, and when it occurred.
 */
@Repository("auditlogweb.AuditlogwebDao")
@RequiredArgsConstructor
public class AuditDao {

    private final SessionFactory sessionFactory;

    private final Logger log = LoggerFactory.getLogger(AuditDao.class);

    /**
     * Retrieves a paginated list of all revisions for a given audited entity class.
     *
     * @param entityClass the audited entity class to retrieve revisions for
     * @param page        the page number (0-based)
     * @param size        the number of results per page
     * @param <T>         the type of the audited entity
     * @return a list of {@link AuditEntity} containing revision data
     */
    @SuppressWarnings("unchecked")
    public <T> List<AuditEntity<T>> getAllRevisions(Class<T> entityClass, int page, int size, String sortOrder) {
        AuditReader auditReader = AuditReaderFactory.get(sessionFactory.getCurrentSession());

        AuditQuery auditQuery = auditReader.createQuery()
                .forRevisionsOfEntity(entityClass, false, true);

        if ("asc".equalsIgnoreCase(sortOrder)) {
            auditQuery.addOrder(org.hibernate.envers.query.AuditEntity.revisionProperty("timestamp").asc());
        } else {
            auditQuery.addOrder(org.hibernate.envers.query.AuditEntity.revisionProperty("timestamp").desc());
        }

        auditQuery.setFirstResult(page * size)
                .setMaxResults(size);

        return (List<AuditEntity<T>>) auditQuery.getResultList().stream().map(result -> {
            Object[] array = (Object[]) result;
            T entity = entityClass.cast(array[0]);
            OpenmrsRevisionEntity revisionEntity = (OpenmrsRevisionEntity) array[1];
            RevisionType revisionType = (RevisionType) array[2];
            Integer userId = revisionEntity.getChangedBy();
            return new AuditEntity<>(entity, revisionEntity, revisionType, userId);
        }).collect(Collectors.toList());
    }

    public <T> List<AuditEntity<T>> getAllRevisions(Class<T> entityClass, int page, int size) {
        return getAllRevisions(entityClass, page, size, "desc");
    }

    /**
     * Counts the total number of revisions for a given audited entity class.
     *
     * @param entityClass the audited entity class
     * @return the total number of revisions as a long value
     */
    public long countAllRevisions(Class<?> entityClass) {
        AuditReader auditReader = AuditReaderFactory.get(sessionFactory.getCurrentSession());

        return (long) auditReader.createQuery()
                .forRevisionsOfEntity(entityClass, false, true)
                .addProjection(org.hibernate.envers.query.AuditEntity.revisionNumber().count())
                .getSingleResult();
    }

    /**
     * Retrieves a specific revision of an entity by its entity ID and revision number.
     *
     * @param entityClass the class of the audited entity
     * @param entityId    the ID of the entity
     * @param revisionId  the revision number to fetch
     * @param <T>         the type of the audited entity
     * @return the entity instance at the specified revision, or {@code null} if not found
     */
    public <T> T getRevisionById(Class<T> entityClass, int entityId, int revisionId) {
        AuditReader auditReader = AuditReaderFactory.get(sessionFactory.getCurrentSession());
        return auditReader.find(entityClass, entityId, revisionId);
    }

    /**
     * Retrieves a specific {@link AuditEntity} that includes revision metadata for
     * a given entity and revision ID.
     *
     * @param entityClass the class of the audited entity
     * @param entityId    the ID of the entity
     * @param revisionId  the revision number to retrieve
     * @param <T>         the type of the audited entity
     * @return an {@link AuditEntity} containing the entity, revision metadata, and user info
     */
    public <T> AuditEntity<T> getAuditEntityRevisionById(Class<T> entityClass, int entityId, int revisionId) {
        AuditReader auditReader = AuditReaderFactory.get(sessionFactory.getCurrentSession());
        AuditQuery auditQuery = auditReader.createQuery()
                .forRevisionsOfEntity(entityClass, false, true)
                .add(org.hibernate.envers.query.AuditEntity.id().eq(entityId))
                .add(org.hibernate.envers.query.AuditEntity.revisionNumber().eq(revisionId));

        Object[] result = (Object[]) auditQuery.getSingleResult();
        T entity = entityClass.cast(result[0]);
        OpenmrsRevisionEntity revisionEntity = (OpenmrsRevisionEntity) result[1];
        RevisionType revisionType = (RevisionType) result[2];
        Integer userId = revisionEntity.getChangedBy();
        return new AuditEntity<>(entity, revisionEntity, revisionType, userId);
    }

    /**
     * Retrieves a paginated list of revisions filtered by user ID and/or date range.
     *
     * @param entityClass the class of the audited entity
     * @param page        the page number (0-based)
     * @param size        the number of records per page
     * @param userId      optional user ID to filter by who made the change
     * @param startDate   optional start date for filtering changes
     * @param endDate     optional end date for filtering changes
     * @param <T>         the type of the audited entity
     * @return a list of {@link AuditEntity} revisions matching the filters
     */
    public <T> List<AuditEntity<T>> getRevisionsWithFilters(
            Class<T> entityClass, int page, int size,
            Integer userId, Date startDate, Date endDate, String sortOrder) {

        AuditReader reader = AuditReaderFactory.get(sessionFactory.getCurrentSession());
        AuditQuery query = EnversUtils.buildFilteredAuditQuery(reader, entityClass, userId, startDate, endDate, page, size, sortOrder);

        List<Object[]> results = query.getResultList();

        return results.stream()
                .map(result -> mapToAuditEntity(entityClass, result))
                .collect(Collectors.toList());
    }

    public <T> List<AuditEntity<T>> getRevisionsWithFilters(
            Class<T> entityClass, int page, int size,
            Integer userId, Date startDate, Date endDate) {
        return getRevisionsWithFilters(entityClass, page, size, userId, startDate, endDate, "desc");
    }

    /**
     * Counts the number of entity revisions that match the given filters.
     *
     * @param entityClass the class of the audited entity
     * @param userId      optional user ID to filter changes by user
     * @param startDate   optional filter to include changes from this date onward
     * @param endDate     optional filter to include changes up to this date
     * @param <T>         the type of the audited entity
     * @return the count of matching revisions
     */
    public <T> long countRevisionsWithFilters(Class<T> entityClass, Integer userId, Date startDate, Date endDate) {
        AuditReader reader = AuditReaderFactory.get(sessionFactory.getCurrentSession());
        AuditQuery query = EnversUtils.buildCountQueryWithFilters(reader, entityClass, userId, startDate, endDate);
        Number countResult = (Number) query.getSingleResult();
        return countResult != null ? countResult.longValue() : 0L;
    }

    /**
     * Helper method to convert raw Envers result arrays into {@link AuditEntity} objects.
     *
     * @param entityClass the audited entity class
     * @param auditResult an Object[] array containing entity, revision, and revision type
     * @param <T>         the type of the audited entity
     * @return a fully populated {@link AuditEntity}
     */
    private <T> AuditEntity<T> mapToAuditEntity(Class<T> entityClass, Object[] auditResult) {
        T entity = entityClass.cast(auditResult[0]);
        OpenmrsRevisionEntity revision = (OpenmrsRevisionEntity) auditResult[1];
        RevisionType type = (RevisionType) auditResult[2];
        return new AuditEntity<>(entity, revision, type, revision.getChangedBy());
    }

    /**
     * Retrieves paginated audit entries across all dynamically discovered audited entity classes.
     *
     * @param page the page number (0-based)
     * @param size number of records per page
     * @param userId optional user ID filter
     * @param startDate optional start date filter
     * @param endDate optional end date filter
     * @return paginated list of {@link AuditEntity} records
     */
    public List<AuditEntity<?>> getAllRevisionsAcrossEntities(int page, int size, Integer userId, Date startDate, Date endDate, String sortOrder) {
        List<Class<?>> classes = getNonAbstractAuditedClasses();
        List<AuditEntity<?>> combined = fetchAcrossEntities(classes, userId, startDate, endDate, sortOrder, page, size);

        combined.sort((a, b) -> {
            int compare = b.getRevisionEntity().getRevisionDate().compareTo(a.getRevisionEntity().getRevisionDate());
            return "asc".equalsIgnoreCase(sortOrder) ? -compare : compare;
        });

        return UtilClass.paginate(combined, page, size);
    }

    /**
     * Counts total audit entries across all dynamically discovered audited entity classes.
     *
     * @param userId optional user ID filter
     * @param startDate optional start date filter
     * @param endDate optional end date filter
     * @return total number of matching audit entries
     */
    public long countRevisionsAcrossEntities(Integer userId, Date startDate, Date endDate) {
        return countAcrossEntities(getNonAbstractAuditedClasses(), userId, startDate, endDate);
    }

    /**
     * Helper method to check if an exception is caused by a missing audit table.
     */
    private boolean isMissingAuditTableException(Throwable ex) {
        Throwable cause = ex;
        while (cause != null) {
            if ((cause instanceof SQLGrammarException || cause instanceof SQLSyntaxErrorException)
                    && cause.getMessage() != null
                    && (cause.getMessage().toLowerCase().contains("doesn't exist")
                    || cause.getMessage().toLowerCase().contains("missing")
                    || cause.getMessage().toLowerCase().contains("unknown table"))) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    /**
     * Retrieves a specific revision of a {@link Role} entity by its role name and revision ID.
     *
     * @param roleName    the name of the role
     * @param revisionId  the revision number
     * @return the {@link Role} instance at the specified revision, or {@code null} if not found
     */
    public Role getRoleRevisionById(String roleName, int revisionId) {
        AuditReader auditReader = AuditReaderFactory.get(sessionFactory.getCurrentSession());
        return auditReader.find(Role.class, roleName, revisionId);
    }

    /**
     * Retrieves a specific revision of a {@link GlobalProperty} by its property name and revision ID.
     *
     * @param propertyName the name of the global property
     * @param revisionId   the revision number
     * @return the {@link GlobalProperty} instance at the specified revision, or {@code null} if not found
     */
    public GlobalProperty getGlobalPropertyRevisionById(String propertyName, int revisionId) {
        AuditReader auditReader = AuditReaderFactory.get(sessionFactory.getCurrentSession());
        return auditReader.find(GlobalProperty.class, propertyName, revisionId);
    }

    /**
     * Retrieves a full {@link AuditEntity} for a specific revision of a {@link Role} entity.
     * Includes role data, revision metadata, and user info.
     *
     * @param roleName   the name of the role
     * @param revisionId the revision number
     * @return an {@link AuditEntity} for the specified revision
     */
    public AuditEntity<Role> getRoleAuditEntityRevisionById(String roleName, int revisionId) {
        AuditReader auditReader = AuditReaderFactory.get(sessionFactory.getCurrentSession());
        AuditQuery auditQuery = auditReader.createQuery()
                .forRevisionsOfEntity(Role.class, false, true)
                .add(org.hibernate.envers.query.AuditEntity.id().eq(roleName))
                .add(org.hibernate.envers.query.AuditEntity.revisionNumber().eq(revisionId));

        Object[] result = (Object[]) auditQuery.getSingleResult();
        Role entity = (Role) result[0];
        OpenmrsRevisionEntity revisionEntity = (OpenmrsRevisionEntity) result[1];
        RevisionType revisionType = (RevisionType) result[2];
        Integer userId = revisionEntity.getChangedBy();
        return new AuditEntity<>(entity, revisionEntity, revisionType, userId);
    }

    /**
     * Retrieves a full {@link AuditEntity} for a specific revision of a {@link GlobalProperty}.
     * Includes property data, revision metadata, and user info.
     *
     * @param propertyName the name of the global property
     * @param revisionId   the revision number
     * @return an {@link AuditEntity} for the specified revision
     */
    public AuditEntity<GlobalProperty> getGlobalPropertyAuditEntityRevisionById(String propertyName, int revisionId) {
        AuditReader auditReader = AuditReaderFactory.get(sessionFactory.getCurrentSession());
        AuditQuery auditQuery = auditReader.createQuery()
                .forRevisionsOfEntity(GlobalProperty.class, false, true)
                .add(org.hibernate.envers.query.AuditEntity.id().eq(propertyName))
                .add(org.hibernate.envers.query.AuditEntity.revisionNumber().eq(revisionId));

        Object[] result = (Object[]) auditQuery.getSingleResult();
        GlobalProperty entity = (GlobalProperty) result[0];
        OpenmrsRevisionEntity revisionEntity = (OpenmrsRevisionEntity) result[1];
        RevisionType revisionType = (RevisionType) result[2];
        Integer userId = revisionEntity.getChangedBy();
        return new AuditEntity<>(entity, revisionEntity, revisionType, userId);
    }

    /**
     * Retrieves the list of classes annotated as audited entities.
     *
     * @return list of class names that are audited and not abstract
     */
    private List<Class<?>> getNonAbstractAuditedClasses() {
        return UtilClass.findClassesWithAnnotation()
                .stream()
                .map(className -> {
                    try {
                        return Class.forName(className);
                    } catch (ClassNotFoundException e) {
                        log.warn("Could not load class: {}", className, e);
                        return null;
                    }
                })
                .filter(clazz -> clazz != null && !Modifier.isAbstract(clazz.getModifiers()))
                .collect(Collectors.toList());
    }

    // NEW overload with entityType filter
    public List<AuditEntity<?>> getAllRevisionsAcrossEntities(int page, int size, Integer userId, Date startDate, Date endDate, String sortOrder, String entityType) {
        List<Class<?>> classes = getNonAbstractAuditedClasses().stream()
                .filter(c -> entityType == null || entityType.isEmpty() || c.getSimpleName().equalsIgnoreCase(entityType))
                .collect(Collectors.toList());

        List<AuditEntity<?>> combined = fetchAcrossEntities(classes, userId, startDate, endDate, sortOrder, page, size);

        combined.sort((a, b) -> {
            int compare = b.getRevisionEntity().getRevisionDate().compareTo(a.getRevisionEntity().getRevisionDate());
            return "asc".equalsIgnoreCase(sortOrder) ? -compare : compare;
        });

        return UtilClass.paginate(combined, page, size);
    }

    // NEW overload for count with entityType
    public long countRevisionsAcrossEntities(Integer userId, Date startDate, Date endDate, String entityType) {
        List<Class<?>> classes = getNonAbstractAuditedClasses().stream()
                .filter(c -> entityType == null || entityType.isEmpty() || c.getSimpleName().equalsIgnoreCase(entityType))
                .collect(Collectors.toList());
        return countAcrossEntities(classes, userId, startDate, endDate);
    }

    private List<AuditEntity<?>> fetchAcrossEntities(List<Class<?>> classes, Integer userId, Date startDate, Date endDate, String sortOrder, int page, int size) {
        List<AuditEntity<?>> combined = new ArrayList<>();
        for (Class<?> clazz : classes) {
            try {
                List<? extends AuditEntity<?>> revisions = getRevisionsWithFilters(
                        clazz, page, size, userId, startDate, endDate, sortOrder);
                combined.addAll(revisions);
            } catch (Exception ex) {
                if (isMissingAuditTableException(ex)) {
                    log.warn("Skipping class {} due to missing audit table or SQL error: {}", clazz.getName(), ex.getMessage());
                } else {
                    log.error("Unexpected error while fetching audit logs for class {}: {}", clazz.getName(), ex.getMessage(), ex);
                }
            }
        }
        return combined;
    }

    private long countAcrossEntities(List<Class<?>> classes, Integer userId, Date startDate, Date endDate) {
        return classes.stream().mapToLong(clazz -> {
            try {
                return countRevisionsWithFilters(clazz, userId, startDate, endDate);
            } catch (NotAuditedException e) {
                log.warn("Class not audited, skipping: {}", clazz.getName());
                return 0L;
            } catch (Exception ex) {
                if (isMissingAuditTableException(ex)) {
                    log.warn("Skipping count for class {} due to missing audit table: {}", clazz.getName(), ex.getMessage());
                    return 0L;
                } else {
                    log.error("Unexpected error while counting audit logs for class {}: {}", clazz.getName(), ex.getMessage(), ex);
                    return 0L;
                }
            }
        }).sum();
    }
}