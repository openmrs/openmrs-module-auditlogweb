/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.auditlogweb.rest;

import lombok.RequiredArgsConstructor;
import org.hibernate.ObjectNotFoundException;
import org.openmrs.GlobalProperty;
import org.openmrs.Role;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.module.auditlogweb.AuditEntity;
import org.openmrs.module.auditlogweb.api.AuditService;
import org.openmrs.module.auditlogweb.api.dto.AuditLogDetailDTO;
import org.openmrs.module.auditlogweb.api.dto.AuditLogResponseDto;
import org.openmrs.module.auditlogweb.api.utils.AuditLogConstants;
import org.openmrs.module.auditlogweb.api.utils.UtilClass;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

import javax.persistence.NoResultException;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * REST controller for exposing audit log entries via the OpenMRS REST API.
 * <p>
 * Provides endpoints to fetch audit logs with optional filtering by user, date range, and entity
 * type. Supports pagination.
 * </p>
 * <p>
 * Security: Access to the logs is controlled by the {@link AuditLogConstants#VIEW_AUDIT_LOGS}
 * privilege.
 * </p>
 *
 * <p>
 * Compatible with OpenMRS Platform 2.7.0 and later.
 * </p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/rest/" + RestConstants.VERSION_1 + "/auditlogs")
public class AuditLogRestController {

    private final AuditService auditService;

    /**
     * Retrieves paginated audit log entries with optional filters:
     * user ID, username, date range, and entity type.
     *
     * @param page       zero-based page index
     * @param size       number of results per page
     * @param userId     optional user ID
     * @param username   optional username (resolved to user ID)
     * @param startDate  optional start date ("dd/MM/yyyy")
     * @param endDate    optional end date ("dd/MM/yyyy")
     * @param entityType optional entity type filter
     * @return a structured response containing audit log entries
     * @throws ResponseStatusException if input is invalid
     */
    @GetMapping
    public AuditLogResponseDto getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Integer userId,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String entityType
    ) {
        if (page < 0) page = 0;
        if (size <= 0) size = 20;

        Date start = UtilClass.parseDate(startDate, false);
        Date end = UtilClass.parseDate(endDate, true);

        Integer effectiveUserId = userId;
        if (effectiveUserId == null && username != null && !username.isEmpty()) {
            effectiveUserId = resolveUserIdFromUsername(username);
            if (effectiveUserId == null) {
                return new AuditLogResponseDto(0, page, 0, Collections.emptyList());
            }
        }

        boolean fullDetails = userId != null || username != null || startDate != null || endDate != null || entityType != null;

        List<AuditLogDetailDTO> auditDetails = auditService.mapAuditEntitiesToDetails(
                auditService.getAllRevisionsAcrossEntitiesWithEntityType(page, size, effectiveUserId, start, end, entityType, "desc")
        );

        if (!fullDetails) {
            auditDetails.forEach(d -> d.setChanges(Collections.emptyList()));
        }

        long total = auditService.countRevisionsAcrossEntitiesWithEntityType(effectiveUserId, start, end, entityType);
        int totalPages = (int) Math.ceil(total / (double) size);

        return new AuditLogResponseDto(Math.toIntExact(total), page, totalPages, auditDetails);
    }

    @GetMapping("/{revisionId}")
    public AuditLogDetailDTO getAuditLogByEntity(
            @PathVariable Integer revisionId,
            @RequestParam() String entityName,
            @RequestParam() String entityId
    ){
        if (entityName.trim().isEmpty() || entityId.trim().isEmpty()) {
            throw new IllegalArgumentException("One or more required parameters are empty");
        }

        Class<?> entityClass = UtilClass.resolveAuditedEntityClass(entityName);
        if (entityClass == null) {
            throw new IllegalArgumentException("Cannot find class for " + entityName);
        }

        Object entityIdVal;
        if (Role.class.isAssignableFrom(entityClass) || GlobalProperty.class.isAssignableFrom(entityClass)) {
            entityIdVal = entityId;
        } else {
            entityIdVal = Integer.parseInt(entityId);
        }

        AuditEntity<?> auditEntity;
        try {
            auditEntity = auditService.getAuditEntityRevisionById(entityClass, entityIdVal, revisionId);
        } catch (NoResultException | ObjectNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No audit revision found for " + entityName + " with id " + entityId, ex);
        }

        return  auditService.mapAuditEntitiesToDetails(Collections.singletonList(auditEntity)).get(0);
    }

    private Integer resolveUserIdFromUsername(String username) {
        User user = Context.getUserService().getUserByUsername(username);
        return user != null ? user.getUserId() : null;
    }
}
