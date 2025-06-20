/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.auditlogweb.api.dao;

import org.hibernate.SessionFactory;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditQuery;
import org.openmrs.api.context.Context;
import org.openmrs.api.db.hibernate.envers.OpenmrsRevisionEntity;
import org.openmrs.module.auditlogweb.AuditEntity;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Data access object (DAO) for retrieving audit log information using Hibernate Envers.
 * This DAO provides methods for fetching entity revisions and revision metadata such as
 * who made the change, what was changed, and when it occurred.
 */
@Repository("auditlogweb.AuditlogwebDao")
public class AuditDao {

    private final SessionFactory sessionFactory;

    public AuditDao(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    /**
     * Retrieves all revision entries for a given audited entity class.
     *
     * @param entityClass the entity class to retrieve revisions for
     * @param <T>         the type of the audited entity
     * @return a list of {@link AuditEntity} objects containing revision data for the entity
     */
    @SuppressWarnings("unchecked")
    public <T> List<AuditEntity<T>> getAllRevisions(Class<T> entityClass) {
        AuditReader auditReader = AuditReaderFactory.get(sessionFactory.getCurrentSession());
        AuditQuery auditQuery = auditReader.createQuery().forRevisionsOfEntity(entityClass, false, true);
        return (List<AuditEntity<T>>) auditQuery.getResultList().stream().map(result -> {
            Object[] array = (Object[]) result;
            T entity = entityClass.cast(array[0]);
            OpenmrsRevisionEntity revisionEntity = (OpenmrsRevisionEntity) array[1];
            RevisionType revisionType = (RevisionType) array[2];
            String changedBy = Context.getUserService().getUser(revisionEntity.getChangedBy()).toString();
            return new AuditEntity<>(entity, revisionEntity, revisionType, changedBy);
        }).collect(Collectors.toList());
    }

    /**
     * Retrieves a specific revision of an entity by its entity ID and revision number.
     *
     * @param entityClass the entity class type
     * @param entityId    the unique ID of the entity
     * @param revisionId  the specific revision number to fetch
     * @param <T>         the type of the audited entity
     * @return the entity instance at the specified revision, or null if not found
     */
    public <T> T getRevisionById(Class<T> entityClass, int entityId, int revisionId) {
        AuditReader auditReader = AuditReaderFactory.get(sessionFactory.getCurrentSession());
        T entity = auditReader.find(entityClass, entityId, revisionId);
        return entity;
    }

    /**
     * Retrieves an {@link AuditEntity} object that includes revision metadata for a given entity
     * and revision ID.
     *
     * @param entityClass the class of the audited entity
     * @param entityId    the ID of the specific entity
     * @param revisionId  the revision number to retrieve
     * @param <T>         the type of the audited entity
     * @return an {@link AuditEntity} object containing the entity, revision info, and author
     */
    public <T> AuditEntity<T> getAuditEntityRevisionById(Class<T> entityClass, int entityId, int revisionId){
        AuditReader auditReader = AuditReaderFactory.get(sessionFactory.getCurrentSession());
        AuditQuery auditQuery = auditReader.createQuery()
                .forRevisionsOfEntity(entityClass, false, true)
                .add(org.hibernate.envers.query.AuditEntity.id().eq(entityId))
                .add(org.hibernate.envers.query.AuditEntity.revisionNumber().eq(revisionId));
        Object[] result = (Object[]) auditQuery.getSingleResult();
        T entity = entityClass.cast(result[0]);
        OpenmrsRevisionEntity revisionEntity = (OpenmrsRevisionEntity) result[1];
        RevisionType revisionType = (RevisionType) result[2];
        String changedBy = Context.getUserService().getUser(revisionEntity.getChangedBy()).toString();
        return new AuditEntity<>(entity, revisionEntity, revisionType, changedBy);
    }
}