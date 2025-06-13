package org.openmrs.module.auditlogweb.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditFieldDiff {
    private String fieldName;
    private String oldValue;
    private String currentValue;
    private boolean changed;
}
