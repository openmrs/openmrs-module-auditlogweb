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
@RequestMapping("/rest/" + RestConstants.VERSION_1 + "/auditlogs/patient")
public class PatientLogRestController {

    private final AuditService auditService;
    private final PatientService patientService;

    /**
     * Retrieves paginated audit log entries for a specific patient.
     * <p>The patient is resolved using one of the provided parameters.
     *
     * @param uuid       the patient's UUID (highest priority)
     * @param identifier any patient identifier string (second priority)
     * @param name       the patient's display/given name,partial match (lowest priority)
     * @param page       zero-based page index (default 0)
     * @param size       number of results per page (default 20)
     * @return {@code AuditLogResponseDto} a structured, paginated response containing the patient's audit log entries
     */
    @GetMapping
    public AuditLogResponseDto getPatientAuditLogs(
            @RequestParam(required = false) String uuid,
            @RequestParam(required = false) String identifier,
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        if (page < 0) page = 0;
        if (size <= 0) size = 20;

        if (uuid == null && identifier == null && name == null) {
            throw new IllegalArgumentException("At least one search parameter must be provided either 'uuid', 'identifier', or 'name'.");
        }

        Patient patient = resolvePatient(uuid, identifier, name);

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
     * @param uuid       optional patient UUID
     * @param identifier optional patient identifier string
     * @param name       optional patient name (partial)
     * @return the resolved {@link Patient}, or {@code null} if none found
     */
    private Patient resolvePatient(String uuid, String identifier, String name) {
        if (uuid != null && !uuid.isEmpty()) {
            return patientService.getPatientByUuid(uuid);
        }

        if (identifier != null && !identifier.isEmpty()) {
            List<Patient> byIdentifier = patientService.getPatients(null, identifier, null, false);
            if (!byIdentifier.isEmpty()) {
                return byIdentifier.get(0);
            }
        }

        if (name != null && !name.isEmpty()) {
            List<Patient> byName = patientService.getPatients(name, null, null, false);
            if (!byName.isEmpty()) {
                return byName.get(0);
            }
        }

        return null;
    }
}
