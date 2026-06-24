package org.openmrs.module.auditlogweb.api.dao;
 
import org.openmrs.module.auditlogweb.ReadAuditLog;
 
public interface ReadAuditDAO {
 
    void saveReadAuditLog(ReadAuditLog readAuditLog);
}