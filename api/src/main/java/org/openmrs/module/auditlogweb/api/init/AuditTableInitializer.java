package org.openmrs.module.auditlogweb.api.init;

import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.openmrs.api.context.Context;
import org.openmrs.module.auditlogweb.api.utils.UtilClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class AuditTableInitializer {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final SessionFactory sessionFactory;

    public AuditTableInitializer(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @PostConstruct
    public void initializeAuditTables() {
        try {
            String property = Context.getRuntimeProperties().getProperty("auditlogweb.runAuditTableInit");
            if (!"true".equalsIgnoreCase(property)) {
                log.info("Audit table initialization skipped (property not set).");
                return;
            }

            SessionFactoryImplementor sfi = (SessionFactoryImplementor) sessionFactory;

            sessionFactory.openSession().doWork(connection -> {
                try (Statement stmt = connection.createStatement()) {
                    List<String> auditedClassNames = UtilClass.findClassesWithAnnotation();

                    @SuppressWarnings("unchecked")
                    Map<String, AbstractEntityPersister> entityPersisters =
                            (Map<String, AbstractEntityPersister>)(Map<?, ?>) sfi.getMetamodel().entityPersisters();

                    Set<String> entityNames = entityPersisters.keySet();

                    // Get Envers sufix/prefix config values for audit table naming
                    String tablePrefix = Context.getRuntimeProperties().getProperty("org.hibernate.envers.audit_table_prefix", "");
                    String tableSuffix = Context.getRuntimeProperties().getProperty("org.hibernate.envers.audit_table_suffix", "_AUD");

                    for (String className : auditedClassNames) {
                        try {
                            if (!entityNames.contains(className)) {
                                log.debug("Skipping non-entity class: {}", className);
                                continue;
                            }

                            AbstractEntityPersister persister = entityPersisters.get(className);
                            String originalTable = persister.getTableName();

                            // Apply prefix and suffix to audit table name
                            String auditTable = tablePrefix + originalTable + tableSuffix;

                            String auditTableEscaped = "`" + auditTable + "`";
                            String originalTableEscaped = "`" + originalTable + "`";

                            String checkSql = "SHOW TABLES LIKE '" + auditTable + "'";
                            try (ResultSet rs = stmt.executeQuery(checkSql)) {
                                if (!rs.next()) {
                                    String createSql = "CREATE TABLE " + auditTableEscaped + " LIKE " + originalTableEscaped;
                                    stmt.executeUpdate(createSql);
                                    log.info("Created missing audit table: {}", auditTable);

                                    stmt.executeUpdate("ALTER TABLE " + auditTableEscaped + " ADD COLUMN rev INT");
                                    stmt.executeUpdate("ALTER TABLE " + auditTableEscaped + " ADD COLUMN revtype TINYINT");
                                } else {
                                    log.debug("Audit table {} already exists.", auditTable);
                                }
                            }

                        } catch (Exception e) {
                            log.warn("Error processing audit table for class: {}: {}", className, e.getMessage());
                        }
                    }
                }
            });

        } catch (Exception e) {
            log.error("Audit table initialization failed: {}", e.getMessage(), e);
        }
    }
}