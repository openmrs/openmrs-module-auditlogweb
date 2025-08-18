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

            // Check if Envers is enabled
            boolean enversEnabled = Boolean.parseBoolean(Context.getRuntimeProperties()
                    .getProperty("hibernate.integration.envers.enabled", "false"));

            if (!enversEnabled) {
                log.info("Envers auditing is not enabled; skipping audit table initialization.");
                return;
            }

            SessionFactoryImplementor sfi = (SessionFactoryImplementor) sessionFactory;

            sessionFactory.openSession().doWork(connection -> {
                try (Statement stmt = connection.createStatement()) {

                    // Create the revision table if not present
                    String revisionTable = Context.getRuntimeProperties()
                            .getProperty("org.hibernate.envers.revision_table_name", "revision_entity");

                    String checkRevTableSql = "SHOW TABLES LIKE '" + revisionTable + "'";
                    try (ResultSet rs = stmt.executeQuery(checkRevTableSql)) {
                        if (!rs.next()) {
                            log.info("Revision table '{}' does not exist. Creating manually.", revisionTable);

                            String createRevisionTableSql =
                                    "CREATE TABLE `" + revisionTable + "` (" +
                                            "id INT NOT NULL AUTO_INCREMENT, " +
                                            "timestamp BIGINT NOT NULL, " +
                                            "changedBy INT NULL, " +
                                            "changedOn DATETIME(6) NULL, " +
                                            "PRIMARY KEY (id)" +
                                            ")";

                            stmt.executeUpdate(createRevisionTableSql);
                            log.info("Revision table '{}' created successfully.", revisionTable);
                        } else {
                            log.debug("Revision table '{}' already exists.", revisionTable);
                        }
                    }

                    // Prepare to create _AUD tables for audited entities
                    List<String> auditedClassNames = UtilClass.findClassesWithAnnotation();

                    @SuppressWarnings("unchecked")
                    Map<String, AbstractEntityPersister> entityPersisters =
                            (Map<String, AbstractEntityPersister>)(Map<?, ?>) sfi.getMetamodel().entityPersisters();

                    Set<String> entityNames = entityPersisters.keySet();

                    // Get audit table prefix/suffix from OpenMRS runtime config
                    String tablePrefix = Context.getRuntimeProperties()
                            .getProperty("org.hibernate.envers.audit_table_prefix", "");
                    String tableSuffix = Context.getRuntimeProperties()
                            .getProperty("org.hibernate.envers.audit_table_suffix", "_AUD");

                    for (String className : auditedClassNames) {
                        try {
                            if (!entityNames.contains(className)) {
                                log.debug("Skipping non-entity class: {}", className);
                                continue;
                            }

                            AbstractEntityPersister persister = entityPersisters.get(className);
                            String originalTable = persister.getTableName();
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