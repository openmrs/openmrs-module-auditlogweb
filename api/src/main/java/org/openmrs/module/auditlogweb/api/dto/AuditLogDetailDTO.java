package org.openmrs.module.auditlogweb.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuditLogDetailDTO {
    private Integer revisionID;
    private String entityType;
    private String eventType;
    private String changedBy;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy HH:mm:ss", timezone = "GMT")
    private Date changedOn;
    private List<AuditFieldDiff> changes;
}
