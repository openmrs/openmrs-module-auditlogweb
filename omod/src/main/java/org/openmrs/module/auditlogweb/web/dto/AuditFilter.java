package org.openmrs.module.auditlogweb.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuditFilter {
    private Integer userId;
    private Date startDate;
    private Date endDate;
}
