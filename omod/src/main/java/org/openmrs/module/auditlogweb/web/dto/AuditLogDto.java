package org.openmrs.module.auditlogweb.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.hibernate.envers.RevisionType;

import java.util.Date;

@Data
@AllArgsConstructor
public class AuditLogDto {
    private Object entity;
    private RevisionType revisionType;
    private String changedBy;
    private Date changedOn;
    private Object revisionEntity;
}