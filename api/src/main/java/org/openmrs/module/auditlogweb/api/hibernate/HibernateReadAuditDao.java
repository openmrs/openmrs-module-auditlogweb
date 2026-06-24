package org.openmrs.module.auditlogweb.api.hibernate;
 
import lombok.RequiredArgsConstructor;
import org.hibernate.SessionFactory;
import org.openmrs.module.auditlogweb.ReadAuditLog;
import org.openmrs.module.auditlogweb.api.dao.ReadAuditDAO;
import org.springframework.stereotype.Repository;

 
@Repository("auditlogweb.ReadAuditDao")
@RequiredArgsConstructor
public class HibernateReadAuditDao implements ReadAuditDAO {
 
    private final SessionFactory sessionFactory;
 
    @Override
    public void saveReadAuditLog(ReadAuditLog readAuditLog) {
        sessionFactory.getCurrentSession().save(readAuditLog);
    }
}
