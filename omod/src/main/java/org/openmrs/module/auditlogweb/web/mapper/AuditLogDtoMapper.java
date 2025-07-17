/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.auditlogweb.web.mapper;

import org.openmrs.module.auditlogweb.AuditEntity;
import org.openmrs.module.auditlogweb.api.AuditService;
import org.openmrs.module.auditlogweb.web.dto.AuditLogDto;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper class responsible for converting {@link AuditEntity} objects
 * into {@link AuditLogDto} objects for use in the UI layer.
 *
 * <p>This class also resolves the username from the user ID stored in
 * each audit entity's revision metadata, using the {@link AuditService}.
 */
@Component
public class AuditLogDtoMapper {

    private final AuditService auditService;

    /**
     * Constructs a new {@code AuditLogDtoMapper} with the given audit service.
     *
     * @param auditService the audit service used to resolve usernames
     */
    public AuditLogDtoMapper(AuditService auditService) {
        this.auditService = auditService;
    }

    /**
     * Converts a single {@link AuditEntity} to an {@link AuditLogDto},
     * resolving the username and extracting revision metadata.
     *
     * @param entity the audit entity to convert
     * @return a populated {@link AuditLogDto} representing the audit entry
     */
    public AuditLogDto toDto(AuditEntity<?> entity) {
        String classSimpleName = (entity.getEntity() != null ? entity.getEntity().getClass().getSimpleName() : "Unknown");
        return new AuditLogDto(
                entity.getEntity(),
                entity.getRevisionType(),
                auditService.resolveUsername(entity.getChangedBy()),
                entity.getRevisionEntity().getChangedOn(),
                entity.getRevisionEntity(),
                classSimpleName
        );
    }

    /**
     * Converts a list of {@link AuditEntity} objects into a list of {@link AuditLogDto} objects.
     *
     * @param entities the list of audit entities to convert
     * @return a list of {@link AuditLogDto} entries
     */
    public List<AuditLogDto> toDtoList(List<AuditEntity<?>> entities) {
        return entities.stream().map(this::toDto).collect(Collectors.toList());
    }
}