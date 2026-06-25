package org.openmrs.module.auditlogweb.api;

import org.openmrs.module.auditlogweb.ReadAuditLog;
import java.util.List;

public interface ReadAuditService {

     void logReadAudit(ReadAuditLog readAuditLog);

     void logReadAudits(List<ReadAuditLog> readAuditLogs);
}