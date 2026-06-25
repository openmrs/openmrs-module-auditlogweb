/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.auditlogweb.api;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
// Thread-scoped holder for HTTP request metadata captured by AuditContextFilter.
public class AuditLogContext {

    private static final ThreadLocal<AuditLogContext> HOLDER = new ThreadLocal<>();

    private String ipAddress;
    private String userAgent;
    private String sessionId;
    private String loggedInUsername;
    private String loggedInUserUUID;

    public static void set(AuditLogContext ctx) {
        HOLDER.set(ctx);
    }

    public static AuditLogContext get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }


}