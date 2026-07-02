/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.auditlogweb.api;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.envers.AuditTable;
import org.hibernate.envers.Audited;
import org.hibernate.metamodel.spi.MetamodelImplementor;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.api.db.hibernate.envers.OpenmrsRevisionEntity;
import org.openmrs.module.auditlogweb.api.utils.EnversUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

@Component("auditlogweb.auditBackfillService")
@RequiredArgsConstructor
public class AuditBackfillService {
	
	private static final Logger log = LoggerFactory.getLogger(AuditBackfillService.class);
	
	public static final String GP_BACKFILL_ENABLED = "auditlogweb.backfillExistingData.enabled";
	
	public static final String GP_BACKFILL_COMPLETED = "auditlogweb.backfillExistingData.completed";
	
	public static final String GP_BACKFILL_REVISION = "auditlogweb.backfillExistingData.revision";
	
	private static final Set<String> ENVERS_TECHNICAL_COLUMNS = new HashSet<>(
	        Arrays.asList("REV", "REVTYPE", "REVEND", "REVEND_TSTMP"));
	
	private final SessionFactory sessionFactory;
	
	/**
	 * Runs the backfill if Envers is enabled, the feature flag is on, and it has not already run.
	 */
	public void backfillExistingDataIfEnabled() {
		if (!EnversUtils.isEnversEnabled()) {
			log.info("Envers is disabled (hibernate.integration.envers.enabled != true); skipping audit backfill.");
			return;
		}
		
		AdministrationService administrationService = Context.getAdministrationService();
		
		if (!Boolean.parseBoolean(administrationService.getGlobalProperty(GP_BACKFILL_ENABLED, "false"))) {
			log.info("{} is not true; skipping audit backfill.", GP_BACKFILL_ENABLED);
			return;
		}
		if (Boolean.parseBoolean(administrationService.getGlobalProperty(GP_BACKFILL_COMPLETED, "false"))) {
			log.info("Audit backfill already completed ({}=true); skipping.", GP_BACKFILL_COMPLETED);
			return;
		}
		
		List<TableMapping> mappings = resolveAuditedTableMappings();
		if (mappings.isEmpty()) {
			log.warn("No audited entities resolved from the metamodel; aborting audit backfill.");
			return;
		}
		
		log.warn("Starting one-time audit backfill of existing data into {} audited tables...", mappings.size());
		Integer revId = reuseRevisionId();
		
		try (Session session = sessionFactory.openSession()) {
			Transaction tx = session.beginTransaction();
			try {
				if (revId == null) {
					revId = createBaselineRevision(session);
				}
				final int revision = revId;
				session.doWork(connection -> runBackfill(connection, mappings, revision));
				tx.commit();
			}
			catch (RuntimeException e) {
				tx.rollback();
				throw e;
			}
		}
		
		administrationService.setGlobalProperty(GP_BACKFILL_REVISION, String.valueOf(revId));
		administrationService.setGlobalProperty(GP_BACKFILL_COMPLETED, "true");
		log.warn("Audit backfill finished at revision {}.", revId);
	}
	
	private List<TableMapping> resolveAuditedTableMappings() {
		SessionFactoryImplementor sfi = sessionFactory.unwrap(SessionFactoryImplementor.class);
		MetamodelImplementor metamodel = sfi.getMetamodel();
		Properties runtimeProperties = Context.getRuntimeProperties();
		String prefix = runtimeProperties.getProperty("org.hibernate.envers.audit_table_prefix", "");
		String suffix = runtimeProperties.getProperty("org.hibernate.envers.audit_table_suffix", "_audit");
		
		List<TableMapping> result = new ArrayList<>();
		Set<String> seenAuditTables = new LinkedHashSet<>();
		for (EntityPersister persister : metamodel.entityPersisters().values()) {
			if (!(persister instanceof AbstractEntityPersister)) {
				continue;
			}
			Class<?> mappedClass = persister.getMappedClass();
			if (mappedClass == null || !mappedClass.isAnnotationPresent(Audited.class)) {
				continue;
			}
			
			AbstractEntityPersister aep = (AbstractEntityPersister) persister;
			String baseTable = unqualifiedTableName(aep.getTableName());
			
			String auditTable;
			AuditTable auditTableAnnotation = mappedClass.getAnnotation(AuditTable.class);
			if (auditTableAnnotation != null && auditTableAnnotation.value() != null
			        && !auditTableAnnotation.value().isEmpty()) {
				auditTable = auditTableAnnotation.value();
			} else {
				auditTable = prefix + baseTable + suffix;
			}
			
			if (seenAuditTables.add(auditTable.toLowerCase(Locale.ROOT))) {
				result.add(new TableMapping(baseTable, auditTable));
			}
		}
		return result;
	}
	
	private Integer reuseRevisionId() {
		String storedRevisionId = Context.getAdministrationService().getGlobalProperty(GP_BACKFILL_REVISION, "");
		if (StringUtils.isBlank(storedRevisionId)) {
			return null;
		}
		try {
			Integer revId = Integer.valueOf(storedRevisionId.trim());
			try (Session session = sessionFactory.openSession()) {
				return session.get(OpenmrsRevisionEntity.class, revId) != null ? revId : null;
			}
		}
		catch (NumberFormatException e) {
			return null;
		}
	}
	
	/**
	 * Determines whether the given revision is the baseline created by the one-time backfill process.
	 */
	public boolean isBaselineRevision(int revisionId) {
		String storedRevisionId = Context.getAdministrationService().getGlobalProperty(GP_BACKFILL_REVISION, "");
		if (StringUtils.isBlank(storedRevisionId)) {
			return false;
		}
		try {
			return Integer.parseInt(storedRevisionId.trim()) == revisionId;
		}
		catch (NumberFormatException e) {
			return false;
		}
	}
	
	private Integer createBaselineRevision(Session session) {
		OpenmrsRevisionEntity revision = new OpenmrsRevisionEntity();
		revision.setTimestamp(System.currentTimeMillis());
		revision.setChangedOn(new Date());
		session.save(revision);
		session.flush();
		return revision.getId();
	}
	
	private void runBackfill(Connection connection, List<TableMapping> mappings, int revId) throws SQLException {
		try (Statement off = connection.createStatement()) {
			off.execute("SET FOREIGN_KEY_CHECKS = 0");
		}
		try {
			for (TableMapping mapping : mappings) {
				try {
					long insertedRows = backfillTable(connection, mapping, revId);
					if (insertedRows > 0) {
						log.info("Audit backfill: {} -> {} ({} rows).", mapping.baseTable, mapping.auditTable, insertedRows);
					}
				}
				catch (Exception e) {
					log.warn("Audit backfill skipped for {} -> {}: {}", mapping.baseTable, mapping.auditTable,
					    describeRootCause(e));
				}
			}
		}
		finally {
			try (Statement on = connection.createStatement()) {
				on.execute("SET FOREIGN_KEY_CHECKS = 1");
			}
		}
	}
	
	private long backfillTable(Connection connection, TableMapping mapping, int revId) throws SQLException {
		DatabaseMetaData md = connection.getMetaData();
		String catalog = connection.getCatalog();
		
		List<String> auditColumns = getColumnNames(md, catalog, mapping.auditTable);
		if (auditColumns.isEmpty()) {
			throw new IllegalStateException("audit columns not found");
		}
		Set<String> baseColumns = toLowerCaseSet(getColumnNames(md, catalog, mapping.baseTable));
		Set<String> auditColumnsLower = toLowerCaseSet(auditColumns);
		
		List<String> dataColumns = new ArrayList<>();
		for (String column : auditColumns) {
			if (ENVERS_TECHNICAL_COLUMNS.contains(column.toUpperCase(Locale.ROOT))) {
				continue;
			}
			if (baseColumns.contains(column.toLowerCase(Locale.ROOT))) {
				dataColumns.add(column);
			}
		}
		if (dataColumns.isEmpty()) {
			throw new IllegalStateException("no common data columns between base and audit table");
		}
		
		boolean hasRevType = auditColumnsLower.contains("revtype");
		List<String> joinColumns = new ArrayList<>();
		for (String pk : getPrimaryKeyColumns(md, catalog, mapping.baseTable)) {
			if (auditColumnsLower.contains(pk.toLowerCase(Locale.ROOT))) {
				joinColumns.add(pk);
			}
		}
		if (joinColumns.isEmpty()) {
			throw new IllegalStateException("no shared key columns between base and audit table");
		}
		
		StringBuilder sql = new StringBuilder("INSERT INTO ").append(quoteIdentifier(mapping.auditTable)).append(" (");
		for (String column : dataColumns) {
			sql.append(quoteIdentifier(column)).append(", ");
		}
		sql.append("REV").append(hasRevType ? ", REVTYPE) SELECT " : ") SELECT ");
		for (String column : dataColumns) {
			sql.append("b.").append(quoteIdentifier(column)).append(", ");
		}
		sql.append(revId).append(hasRevType ? ", 0" : "").append(" FROM ").append(quoteIdentifier(mapping.baseTable))
		        .append(" b WHERE NOT EXISTS (SELECT 1 FROM ").append(quoteIdentifier(mapping.auditTable))
		        .append(" a WHERE ");
		for (int i = 0; i < joinColumns.size(); i++) {
			if (i > 0) {
				sql.append(" AND ");
			}
			sql.append("a.").append(quoteIdentifier(joinColumns.get(i))).append(" = b.")
			        .append(quoteIdentifier(joinColumns.get(i)));
		}
		sql.append(")");
		
		try (PreparedStatement ps = connection.prepareStatement(sql.toString())) {
			return ps.executeUpdate();
		}
	}
	
	private List<String> getColumnNames(DatabaseMetaData md, String catalog, String table) throws SQLException {
		List<String> columns = new ArrayList<>();
		try (ResultSet rs = md.getColumns(catalog, null, table, "%")) {
			while (rs.next()) {
				columns.add(rs.getString("COLUMN_NAME"));
			}
		}
		return columns;
	}
	
	private List<String> getPrimaryKeyColumns(DatabaseMetaData md, String catalog, String table) throws SQLException {
		TreeMap<Short, String> ordered = new TreeMap<>();
		try (ResultSet rs = md.getPrimaryKeys(catalog, null, table)) {
			while (rs.next()) {
				ordered.put(rs.getShort("KEY_SEQ"), rs.getString("COLUMN_NAME"));
			}
		}
		return new ArrayList<>(ordered.values());
	}
	
	private Set<String> toLowerCaseSet(List<String> values) {
		Set<String> set = new HashSet<>();
		for (String value : values) {
			set.add(value.toLowerCase(Locale.ROOT));
		}
		return set;
	}
	
	private String unqualifiedTableName(String tableName) {
		String name = tableName.replace("`", "").replace("\"", "");
		int dot = name.lastIndexOf('.');
		return dot >= 0 ? name.substring(dot + 1) : name;
	}
	
	private String quoteIdentifier(String identifier) {
		return "`" + identifier.replace("`", "``") + "`";
	}
	
	private String describeRootCause(Throwable t) {
		Throwable cause = t;
		while (cause.getCause() != null && cause.getCause() != cause) {
			cause = cause.getCause();
		}
		return cause.getClass().getSimpleName() + ": " + cause.getMessage();
	}
	
	/** Resolved base/audit table pair for one audited entity. */
	private static final class TableMapping {
		
		private final String baseTable;
		
		private final String auditTable;
		
		private TableMapping(String baseTable, String auditTable) {
			this.baseTable = baseTable;
			this.auditTable = auditTable;
		}
	}
	
}
