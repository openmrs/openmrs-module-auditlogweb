package org.openmrs.module.auditlogweb.api.impl;

import lombok.RequiredArgsConstructor;
import org.openmrs.module.auditlogweb.ReadAuditLog;
import org.openmrs.module.auditlogweb.api.ReadAuditService;
import org.openmrs.module.auditlogweb.api.dao.ReadAuditDAO;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
public class ReadAuditServiceImpl implements ReadAuditService {

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
}