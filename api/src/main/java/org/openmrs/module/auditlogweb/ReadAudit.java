package org.openmrs.module.auditlogweb;

import org.openmrs.BaseOpenmrsObject;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "read_audit")
public class ReadAudit extends BaseOpenmrsObject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    @Column(name = "read_audit_id")
    private Integer id;

    @Column(name = "entity_name", nullable = false)
    private String entityName;

    @Column(name = "entity_id")
    private String entityId;

    @Column(name = "entity_uuid", length = 38)
    private String entityUuid;

    @Column(name = "returns_list")
    private boolean isReturnsList;

    @Column(name = "read_success")
    private boolean isReadSuccess;

    @Column(name = "error_type")
    private String errorType;

    @Column(name = "username", length = 50)
    private String username;

    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "event_time", nullable = false)
    private Date eventTime;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "session_id", length = 100)
    private String sessionId;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @Override
    public Integer getId() {
        return 0;
    }

    @Override
    public void setId(Integer integer) {

    }

}
