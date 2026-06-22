package org.openmrs.module.auditlogweb;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.Column;
import javax.persistence.GenerationType;
import javax.persistence.ManyToOne;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;

@Entity
@Table(name = "read_audit_entity_metadata")
@Getter
@Setter
public class ReadAuditEntityMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "read_audit_id", nullable = false)
    private ReadAuditLog readAuditLog;

    @Column(name = "entity_uuid", nullable = false)
    private String entityUuid;

}
