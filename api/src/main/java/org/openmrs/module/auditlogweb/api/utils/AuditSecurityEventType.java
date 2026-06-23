/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.auditlogweb.api.utils;

/**
 * Strongly typed security audit event categories.
 */
public enum AuditSecurityEventType {
    LOGIN_SUCCESS,
    LOGIN_FAILURE,
    ACCOUNT_LOCKED,
    LOGOUT,
    SESSION_TIMEOUT,
    PASSWORD_RESET_REQUEST_SUCCESS,
    PASSWORD_RESET_REQUEST_FAILURE,
    PASSWORD_RESET_SUCCESS,
    PASSWORD_RESET_FAILURE,
    PASSWORD_CHANGED_SUCCESS,
    PASSWORD_CHANGED_FAILURE,
    UNKNOWN;

    public static AuditSecurityEventType fromName(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return AuditSecurityEventType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
}
