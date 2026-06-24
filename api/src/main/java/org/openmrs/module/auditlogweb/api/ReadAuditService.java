package org.openmrs.module.auditlogweb.api;

import org.openmrs.module.auditlogweb.ReadAuditLog;

public interface ReadAuditService {

     void logReadAudit(ReadAuditLog readAuditLog);
}