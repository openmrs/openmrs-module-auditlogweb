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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
		if (revId == null) {
			revId = createBaselineRevision();
			administrationService.setGlobalProperty(GP_BACKFILL_REVISION, String.valueOf(revId));
		}
		final int revision = revId;
		
		boolean allSucceeded = true;
		try (Session session = sessionFactory.openSession()) {
			// Insert parent audit tables before their children: joined-subclass inheritance gives child
			// audit tables a (id, REV) foreign key to the parent audit table (e.g. patient_aud -> person_aud),
			// so the parent's baseline rows must be committed first.
			List<TableMapping> orderedMappings = session
			        .doReturningWork(connection -> orderByAuditTableDependencies(mappings, connection));
			for (TableMapping mapping : orderedMappings) {
				Transaction tx = session.beginTransaction();
				try {
					long insertedRows = session.doReturningWork(connection -> backfillTable(connection, mapping, revision));
					tx.commit();
					if (insertedRows > 0) {
						log.info("Audit backfill: {} -> {} ({} rows).", mapping.baseTable, mapping.auditTable, insertedRows);
					}
				}
				catch (Exception e) {
					safeRollback(tx);
					allSucceeded = false;
					log.warn("Audit backfill skipped for {} -> {}: {}", mapping.baseTable, mapping.auditTable,
					    describeRootCause(e));
				}
			}
		}
		
		if (allSucceeded) {
			administrationService.setGlobalProperty(GP_BACKFILL_COMPLETED, "true");
			log.warn("Audit backfill finished at revision {}.", revision);
		} else {
			log.warn("Audit backfill did not complete for all tables; {} stays false so it resumes on next startup.",
			    GP_BACKFILL_COMPLETED);
		}
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
	
	/**
	 * Orders the mappings so that any audit table is preceded by the audit tables it references via
	 * foreign keys (parents first). This matters for joined-subclass inheritance, where a child audit
	 * table has a composite (id, REV) foreign key to its parent audit table.
	 */
	private List<TableMapping> orderByAuditTableDependencies(List<TableMapping> mappings, Connection connection)
	        throws SQLException {
		DatabaseMetaData md = connection.getMetaData();
		String catalog = connection.getCatalog();
		
		Set<String> auditTableNames = new HashSet<>();
		for (TableMapping mapping : mappings) {
			auditTableNames.add(mapping.auditTable.toLowerCase(Locale.ROOT));
		}
		
		Map<String, Set<String>> parentsByAuditTable = new HashMap<>();
		for (TableMapping mapping : mappings) {
			String child = mapping.auditTable.toLowerCase(Locale.ROOT);
			Set<String> parents = new HashSet<>();
			try (ResultSet rs = md.getImportedKeys(catalog, null, mapping.auditTable)) {
				while (rs.next()) {
					String referenced = rs.getString("PKTABLE_NAME");
					if (referenced == null) {
						continue;
					}
					String parent = referenced.toLowerCase(Locale.ROOT);
					if (!parent.equals(child) && auditTableNames.contains(parent)) {
						parents.add(parent);
					}
				}
			}
			parentsByAuditTable.put(child, parents);
		}
		
		List<TableMapping> ordered = new ArrayList<>();
		Set<String> emitted = new HashSet<>();
		List<TableMapping> remaining = new ArrayList<>(mappings);
		boolean progress = true;
		while (!remaining.isEmpty() && progress) {
			progress = false;
			Iterator<TableMapping> it = remaining.iterator();
			while (it.hasNext()) {
				TableMapping mapping = it.next();
				String name = mapping.auditTable.toLowerCase(Locale.ROOT);
				if (emitted.containsAll(parentsByAuditTable.get(name))) {
					ordered.add(mapping);
					emitted.add(name);
					it.remove();
					progress = true;
				}
			}
		}
		ordered.addAll(remaining);
		return ordered;
	}
	
	Integer reuseRevisionId() {
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
	
	private Integer createBaselineRevision() {
		try (Session session = sessionFactory.openSession()) {
			Transaction tx = session.beginTransaction();
			try {
				OpenmrsRevisionEntity revision = new OpenmrsRevisionEntity();
				revision.setTimestamp(System.currentTimeMillis());
				revision.setChangedOn(new Date());
				session.save(revision);
				tx.commit();
				return revision.getId();
			}
			catch (RuntimeException e) {
				safeRollback(tx);
				throw e;
			}
		}
	}
	
	private void safeRollback(Transaction tx) {
		try {
			if (tx != null && tx.isActive()) {
				tx.rollback();
			}
		}
		catch (RuntimeException e) {
			log.warn("Rollback failed during audit backfill: {}", describeRootCause(e));
		}
	}
	
	private long backfillTable(Connection connection, TableMapping mapping, int revId) throws SQLException {
		DatabaseMetaData md = connection.getMetaData();
		String catalog = connection.getCatalog();
		String quote = md.getIdentifierQuoteString();
		if (quote == null || " ".equals(quote)) {
			quote = "";
		}
		
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
		
		StringBuilder sql = new StringBuilder("INSERT INTO ").append(quoteIdentifier(mapping.auditTable, quote))
		        .append(" (");
		for (String column : dataColumns) {
			sql.append(quoteIdentifier(column, quote)).append(", ");
		}
		sql.append("REV").append(hasRevType ? ", REVTYPE) SELECT " : ") SELECT ");
		for (String column : dataColumns) {
			sql.append("b.").append(quoteIdentifier(column, quote)).append(", ");
		}
		sql.append(revId).append(hasRevType ? ", 0" : "").append(" FROM ").append(quoteIdentifier(mapping.baseTable, quote))
		        .append(" b WHERE NOT EXISTS (SELECT 1 FROM ").append(quoteIdentifier(mapping.auditTable, quote))
		        .append(" a WHERE ");
		for (int i = 0; i < joinColumns.size(); i++) {
			if (i > 0) {
				sql.append(" AND ");
			}
			sql.append("a.").append(quoteIdentifier(joinColumns.get(i), quote)).append(" = b.")
			        .append(quoteIdentifier(joinColumns.get(i), quote));
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
	
	private String quoteIdentifier(String identifier, String quote) {
		if (quote.isEmpty()) {
			return identifier;
		}
		return quote + identifier.replace(quote, quote + quote) + quote;
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
