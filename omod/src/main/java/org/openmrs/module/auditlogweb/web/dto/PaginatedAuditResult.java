package org.openmrs.module.auditlogweb.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.openmrs.module.auditlogweb.AuditEntity;

import java.util.List;

@Data
@AllArgsConstructor
public class PaginatedAuditResult {
    private List<AuditEntity<?>> audits;
    private long totalCount;
}
