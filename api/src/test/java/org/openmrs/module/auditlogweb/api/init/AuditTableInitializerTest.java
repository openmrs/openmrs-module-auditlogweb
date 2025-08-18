package org.openmrs.module.auditlogweb.api.init;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.spi.MetamodelImplementor;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.junit.Test;
import org.openmrs.api.context.Context;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;

public class AuditTableInitializerTest {

    @Test
    public void testInitializeAuditTables_runsWithoutError() throws Exception {
        SessionFactory sessionFactory = mock(SessionFactoryImplementor.class);
        Session session = mock(Session.class);
        Connection connection = mock(Connection.class);
        Statement stmt = mock(Statement.class);
        ResultSet rs = mock(ResultSet.class);

        when(sessionFactory.openSession()).thenReturn(session);
        doAnswer(invocation -> {
            ((org.hibernate.jdbc.Work) invocation.getArgument(0)).execute(connection);
            return null;
        }).when(session).doWork(any());

        when(connection.createStatement()).thenReturn(stmt);
        when(stmt.executeQuery(anyString())).thenReturn(rs);
        when(rs.next()).thenReturn(false); // Simulate missing tables

        SessionFactoryImplementor sfi = (SessionFactoryImplementor) sessionFactory;
        MetamodelImplementor metamodel = mock(MetamodelImplementor.class);
        when(sfi.getMetamodel()).thenReturn(metamodel);

        AbstractEntityPersister persister = mock(AbstractEntityPersister.class);
        when(persister.getTableName()).thenReturn("test_table");

        Map<String, AbstractEntityPersister> map = Collections.singletonMap("Patient", persister);
        when(metamodel.entityPersisters()).thenReturn((Map) map);

        Properties mockProps = new Properties();
        mockProps.setProperty("auditlogweb.runAuditTableInit", "true");
        mockProps.setProperty("hibernate.integration.envers.enabled", "true");
        mockProps.setProperty("org.hibernate.envers.audit_table_prefix", "");
        mockProps.setProperty("org.hibernate.envers.audit_table_suffix", "_AUD");
        mockProps.setProperty("org.hibernate.envers.revision_table_name", "revision_entity");

        Context.setRuntimeProperties(mockProps);

        AuditTableInitializer initializer = new AuditTableInitializer(sessionFactory);
        initializer.initializeAuditTables();
    }
}