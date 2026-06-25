package org.openmrs.module.auditlogweb;

import lombok.Getter;
import lombok.Setter;
import org.openmrs.BaseOpenmrsObject;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Column;
import javax.persistence.OneToMany;
import javax.persistence.CascadeType;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "read_audit_log")
@Getter
@Setter
public class ReadAuditLog extends BaseOpenmrsObject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "entity_name", nullable = false)
    private String entityName;

    @OneToMany(mappedBy = "readAuditLog", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReadAuditEntityMetadata> targets = new ArrayList<>();

    @Column(name = "read_success")
    private boolean isReadSuccess;

    @Column(name = "username", length = 50)
    private String username;

    @Column(name = "user_uuid", length = 38, nullable = false)
    private String userUUID;

    @Column(name = "event_time", nullable = false)
    private Date eventTime;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "session_id", length = 100)
    private String sessionId;

    public void addTarget(ReadAuditEntityMetadata target) {
        targets.add(target);
        target.setReadAuditLog(this);
    }

    public void removeTarget(ReadAuditEntityMetadata target) {
        targets.remove(target);
        target.setReadAuditLog(null);
    }
}
