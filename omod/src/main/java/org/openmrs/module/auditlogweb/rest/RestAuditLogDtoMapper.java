/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.auditlogweb.rest;

import lombok.RequiredArgsConstructor;
import org.openmrs.module.auditlogweb.AuditEntity;
import org.openmrs.module.auditlogweb.api.dto.RestAuditLogDto;
import org.openmrs.module.auditlogweb.web.dto.AuditLogDto;
import org.openmrs.module.auditlogweb.web.mapper.AuditLogDtoMapper;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RestAuditLogDtoMapper {

    private final AuditLogDtoMapper baseMapper;

    public RestAuditLogDto toDto(AuditEntity<?> entity) {
        AuditLogDto base = baseMapper.toDto(entity);

        // Convert Date to formatted String
        String formattedChangedOn = "";
        if (base.getChangedOn() != null) {
            formattedChangedOn = DateTimeFormatter
                    .ofPattern("yyyy-MM-dd HH:mm:ss z")
                    .withZone(ZoneId.of("GMT"))
                    .format(base.getChangedOn().toInstant());
        }

        return new RestAuditLogDto(
                base.getEntityClassSimpleName(),
                base.getEntityIdentifier(),
                base.getRevisionType().name(),
                base.getChangedBy(),
                formattedChangedOn  // <-- fix: this is now a String
        );
    }

    public List<RestAuditLogDto> toDtoList(List<AuditEntity<?>> entities) {
        return entities.stream().map(this::toDto).collect(Collectors.toList());
    }
}
