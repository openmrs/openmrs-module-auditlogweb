package org.openmrs.module.auditlogweb.api.impl;
 
import lombok.RequiredArgsConstructor;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.auditlogweb.ReadAuditLog;
import org.openmrs.module.auditlogweb.api.ReadAuditService;
import org.openmrs.module.auditlogweb.api.dao.ReadAuditDAO;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@RequiredArgsConstructor
public class ReadAuditServiceImpl extends BaseOpenmrsService implements ReadAuditService {

    private final ReadAuditDAO readAuditDAO;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logReadAudit(ReadAuditLog readAuditLog) {
        readAuditDAO.saveReadAuditLog(readAuditLog);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logReadAudits(List<ReadAuditLog> readAuditLogs) {
        if (readAuditLogs != null) {
            for (ReadAuditLog log : readAuditLogs) {
                readAuditDAO.saveReadAuditLog(log);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReadAuditLog> getReadAuditLogs(String eventType, String username, Date startDate, Date endDate, int page, int size) {
        return readAuditDAO.getReadAuditLogs(eventType, username, startDate, endDate, page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public long countReadAuditLogs(String eventType, String username, Date startDate, Date endDate) {
        return readAuditDAO.countReadAuditLogs(eventType, username, startDate, endDate);
    }

    @Override
    public ReadAuditLog getReadAuditLogById(Integer id) {
        return readAuditDAO.getReadAuditLogById(id);
    }

    @Override
    public ReadAuditLog getReadAuditLogByUUID(String UUID) {
        return readAuditDAO.getReadAuditLogByUUID(UUID);
    }

    @Override
    public List<ReadAuditLog> getRelatedReadLogs(String sessionId, int limit) {
        return readAuditDAO.getRelatedReadLogs(sessionId, limit);
    }
}