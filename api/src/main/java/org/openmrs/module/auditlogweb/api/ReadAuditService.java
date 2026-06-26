package org.openmrs.module.auditlogweb.api;

import org.openmrs.annotation.Authorized;
import org.openmrs.module.auditlogweb.ReadAuditLog;
import org.openmrs.module.auditlogweb.api.utils.AuditLogConstants;

import java.util.Date;
import java.util.List;

public interface ReadAuditService {

     void logReadAudit(ReadAuditLog readAuditLog);

     void logReadAudits(List<ReadAuditLog> readAuditLogs);

     @Authorized(AuditLogConstants.VIEW_READ_AUDIT_LOGS)
     List<ReadAuditLog> getReadAuditLogs(String eventType, String username,
                                         Date startDate, Date endDate, int page, int size);

     @Authorized(AuditLogConstants.VIEW_READ_AUDIT_LOGS)
     long countReadAuditLogs(String eventType, String username, Date startDate, Date endDate);

     @Authorized(AuditLogConstants.VIEW_READ_AUDIT_LOGS)
     ReadAuditLog getReadAuditLogById(Integer id);

     @Authorized(AuditLogConstants.VIEW_READ_AUDIT_LOGS)
     ReadAuditLog getReadAuditLogByUUID(String UUID);

     @Authorized(AuditLogConstants.VIEW_READ_AUDIT_LOGS)
     List<ReadAuditLog> getRelatedReadLogs(String sessionId, int limit);
}