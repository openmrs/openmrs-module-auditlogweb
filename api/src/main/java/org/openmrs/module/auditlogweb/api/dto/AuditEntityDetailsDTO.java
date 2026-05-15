/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.auditlogweb.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Date;
import java.util.List;

// It used to get the audit of entity for a single revision which is what changed on that revision for the given
// entity type and also the related entity responsible who got involved in that revision id.

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuditEntityDetailsDTO {

    private Integer revisionID;
    private String entityType;
    private String eventType;
    private String changedBy;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy HH:mm:ss", timezone = "GMT")
    private Date changedOn;
    private List<AuditFieldDiff> changes;
    private List<RelatedEntityDto> relatedEntities;
}
