/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.auditlogweb;

import lombok.Getter;
import lombok.Setter;
import org.openmrs.BaseOpenmrsObject;
import org.openmrs.module.auditlogweb.api.utils.AuditSecurityEventType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Enumerated;
import javax.persistence.EnumType;
import javax.persistence.Table;
import java.util.Date;

/**
 * Hibernate entity representing a single security-related audit event. 
 */
@Entity
@Table(name = "audit_security_event")
@Getter
@Setter
public class AuditSecurityEvent extends BaseOpenmrsObject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * One of: LOGIN_SUCCESS, LOGIN_FAILURE, ACCOUNT_LOCKED, LOGOUT,
     * SESSION_TIMEOUT, PASSWORD_RESET_REQUEST_SUCCESS, PASSWORD_RESET_REQUEST_FAILURE,
     * PASSWORD_RESET_SUCCESS, PASSWORD_RESET_FAILURE, PASSWORD_CHANGED_SUCCESS,
     * PASSWORD_CHANGED_FAILURE.
     */
    @Column(name = "event_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private AuditSecurityEventType eventType;

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

}
