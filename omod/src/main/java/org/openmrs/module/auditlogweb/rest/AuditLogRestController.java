/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.auditlogweb.rest;

import org.openmrs.annotation.Authorized;
import org.openmrs.module.auditlogweb.api.AuditService;
import org.openmrs.module.auditlogweb.api.dto.AuditLogResponseDto;
import org.openmrs.module.auditlogweb.api.dto.RestAuditLogDto;
import org.openmrs.module.auditlogweb.api.utils.AuditLogConstants;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/rest/" + RestConstants.VERSION_1 + "/auditlogs")
public class AuditLogRestController {

    private final AuditService auditService;

    public AuditLogRestController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    @Authorized(AuditLogConstants.VIEW_AUDIT_LOGS)
    public AuditLogResponseDto getAllAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        List<RestAuditLogDto> logs = auditService.getAllAuditLogs(page, size);
        int totalLogs = (int) auditService.getAuditLogsCount();

        return new AuditLogResponseDto(totalLogs, page, size, logs);
    }
}