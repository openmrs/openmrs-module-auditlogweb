/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.auditlogweb;

import org.hibernate.envers.RevisionType;
import org.openmrs.api.db.hibernate.envers.OpenmrsRevisionEntity;

public class AuditEntity<T> {

    private T entity;

    private OpenmrsRevisionEntity revisionEntity;

    private RevisionType revisionType;

    private String changedBy;

    public AuditEntity(T entity) {
        this.entity = entity;
    }

    public AuditEntity(T entity, OpenmrsRevisionEntity revisionEntity, RevisionType revisionType, String changedBy) {
        this.entity = entity;
        this.revisionEntity = revisionEntity;
        this.revisionType = revisionType;
        this.changedBy = changedBy;
    }

    public T getEntity() {
        return entity;
    }

    public void setEntity(T entity) {
        this.entity = entity;
    }

    public OpenmrsRevisionEntity getRevisionEntity() {
        return revisionEntity;
    }

    public void setRevisionEntity(OpenmrsRevisionEntity revisionEntity) {
        this.revisionEntity = revisionEntity;
    }

    public RevisionType getRevisionType() {
        return revisionType;
    }

    public void setRevisionType(RevisionType revisionType) {
        this.revisionType = revisionType;
    }

    public String getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(String changedBy) {
        this.changedBy = changedBy;
    }
}
