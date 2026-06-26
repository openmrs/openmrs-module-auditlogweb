package org.openmrs.module.auditlogweb.api.dao;
 
import org.openmrs.module.auditlogweb.ReadAuditLog;

import java.util.Date;
import java.util.List;

public interface ReadAuditDAO {
 
    void saveReadAuditLog(ReadAuditLog readAuditLog);

    List<ReadAuditLog> getReadAuditLogs(String entityType, String username,
                                        Date startDate, Date endDate, int page, int size);

    long countReadAuditLogs(String entityType, String username, Date startDate, Date endDate);

    ReadAuditLog getReadAuditLogById(Integer id);

    ReadAuditLog getReadAuditLogByUUID(String uuid);

    List<ReadAuditLog> getRelatedReadLogs(String sessionId, int limit);
}