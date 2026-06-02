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
import org.openmrs.Patient;
import org.openmrs.api.PatientService;
import org.openmrs.module.auditlogweb.AuditEntity;
import org.openmrs.module.auditlogweb.api.AuditService;
import org.openmrs.module.auditlogweb.api.dto.AuditLogDetailDTO;
import org.openmrs.module.auditlogweb.api.dto.AuditLogResponseDto;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/rest/" + RestConstants.VERSION_1 + "/auditlogs/patients")
public class PatientLogRestController {

    private final AuditService auditService;
    private final PatientService patientService;

    /**
     * Retrieves paginated audit log entries for a specific patient.
     * <p>The patient is resolved using one of the provided parameters.
     *
     * @param uuid       the patient's UUID (first priority)
     * @param id         the patient Id
       Either one of these param should be there on request
     * @param page       zero-based page index (default 0)
     * @param size       number of results per page (default 20)
     * @return {@code AuditLogResponseDto} a structured, paginated response containing the patient's audit log entries
     */
    @GetMapping
    public AuditLogResponseDto getPatientAuditLogs(
            @RequestParam(required = false) String uuid,
            @RequestParam(required = false) Integer id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        if (page < 0) page = 0;
        if (size <= 0) size = 20;

        if ((uuid == null || uuid.trim().isEmpty()) && id == null) {
            throw new IllegalArgumentException(
                    "At least one search parameter must be provided either uuid or id."
            );
        }

        Patient patient = resolvePatient(uuid, id);

        if (patient == null) {
            throw new IllegalArgumentException("No patient found for the given search criteria.");
        }

        Integer patientId = patient.getPatientId();

        List<AuditEntity<?>> revisions = auditService.getEntityAuditRevisionsById(patientId, patient.getClass(), page, size, "desc" );

        List<AuditLogDetailDTO> logs = auditService.getEntityDetailedAudit(revisions, patient.getClass());

        long total = auditService.countEntityAuditRevisionsById(patientId, patient.getClass());
        int totalPages = (int) Math.ceil(total / (double) size);

        return new AuditLogResponseDto(Math.toIntExact(total), page, totalPages, logs);
    }

    /**
     * It gets the {@link Patient} from the given search parameters.
     *
     * <p>Search priority:
     * <ol>
     *   <li>UUID          exact match via {@link PatientService#getPatientByUuid(String)}</li>
     *   <li>Identifier    search across all identifier types; first match is used</li>
     *   <li>Name          partial name search; first match is used</li>
     * </ol>
     *
     * @param uuid       patient UUID
     * @param id         patient Id
     * @return the resolved {@link Patient}, or {@code null} if none found
     */
    private Patient resolvePatient(String uuid, Integer id) {
        if (uuid != null && !uuid.isEmpty()) {
            return patientService.getPatientByUuid(uuid);
        }

        return patientService.getPatient(id);
    }
}
