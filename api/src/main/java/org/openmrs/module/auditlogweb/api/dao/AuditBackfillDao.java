/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.auditlogweb.api.dao;

import lombok.RequiredArgsConstructor;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.envers.AuditTable;
import org.hibernate.envers.Audited;
import org.hibernate.metamodel.spi.MetamodelImplementor;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.openmrs.api.context.Context;
import org.openmrs.api.db.hibernate.envers.OpenmrsRevisionEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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

/**
 * DAO class for the audit backfill
 */
@Repository("auditlogweb.auditBackfillDao")
@RequiredArgsConstructor
public class AuditBackfillDao {
	
	private static final Logger log = LoggerFactory.getLogger(AuditBackfillDao.class);
	
	private static final Set<String> ENVERS_TECHNICAL_COLUMNS = new HashSet<>(
	        Arrays.asList("REV", "REVTYPE", "REVEND", "REVEND_TSTMP"));
	
	private final SessionFactory sessionFactory;
	
	/**
	 * Resolves the base/audit table pairs for every {@code @Audited} entity in the metamodel, honouring
	 * the configurable Envers table prefix/suffix and any {@code @AuditTable} override.
	 */
	public List<TableMapping> resolveAuditedTableMappings() {
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
			String auditTable = deriveAuditTableName(mappedClass, baseTable, prefix, suffix);
			
			if (seenAuditTables.add(auditTable.toLowerCase(Locale.ROOT))) {
				result.add(new TableMapping(baseTable, auditTable));
			}
		}
		return result;
	}
	
	/**
	 * Derives the audit table name for an entity: the {@code @AuditTable} value if present, otherwise
	 * {@code prefix + baseTable + suffix}.
	 */
	String deriveAuditTableName(Class<?> mappedClass, String baseTable, String prefix, String suffix) {
		AuditTable auditTableAnnotation = mappedClass.getAnnotation(AuditTable.class);
		if (auditTableAnnotation != null && auditTableAnnotation.value() != null
		        && !auditTableAnnotation.value().isEmpty()) {
			return auditTableAnnotation.value();
		}
		return prefix + baseTable + suffix;
	}
	
	/**
	 * Orders the mappings so that any audit table is preceded by the audit tables it references via
	 * foreign keys (parents first). This matters for joined-subclass inheritance, where a child audit
	 * table has a composite (id, REV) foreign key to its parent audit table.
	 */
	public List<TableMapping> orderByAuditTableDependencies(List<TableMapping> mappings) {
		try (Session session = sessionFactory.openSession()) {
			return session.doReturningWork(
			    connection -> orderParentsBeforeChildren(mappings, readAuditTableParents(mappings, connection)));
		}
	}
	
	private Map<String, Set<String>> readAuditTableParents(List<TableMapping> mappings, Connection connection)
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
		return parentsByAuditTable;
	}
	
	/**
	 * Reorders the mappings so that every audit table comes after the audit tables it depends on (its
	 * parents).
	 */
	List<TableMapping> orderParentsBeforeChildren(List<TableMapping> mappings,
	        Map<String, Set<String>> parentsByAuditTable) {
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
				Set<String> parents = parentsByAuditTable.getOrDefault(name, Collections.emptySet());
				if (emitted.containsAll(parents)) {
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
	
	/** Whether a revision_entity row with the given id exists. */
	public boolean revisionExists(int revisionId) {
		try (Session session = sessionFactory.openSession()) {
			return session.get(OpenmrsRevisionEntity.class, revisionId) != null;
		}
	}
	
	/** Creates and commits a single baseline revision row and returns its generated id. */
	public int createBaselineRevision() {
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
	
	/**
	 * Copies rows that are not yet present in the audit table, stamping them with the baseline
	 * revision. Runs in its own transaction so a failure is bounded to this table.
	 * 
	 * @return the number of rows inserted
	 */
	public long backfillTable(TableMapping mapping, int revisionId) {
		try (Session session = sessionFactory.openSession()) {
			Transaction tx = session.beginTransaction();
			try {
				long insertedRows = session.doReturningWork(connection -> executeBackfill(connection, mapping, revisionId));
				tx.commit();
				return insertedRows;
			}
			catch (RuntimeException e) {
				safeRollback(tx);
				throw e;
			}
		}
	}
	
	private long executeBackfill(Connection connection, TableMapping mapping, int revId) throws SQLException {
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
		
		String sql = buildBackfillInsertSql(mapping, dataColumns, joinColumns, hasRevType, revId, quote);
		try (PreparedStatement ps = connection.prepareStatement(sql)) {
			return ps.executeUpdate();
		}
	}
	
	/**
	 * Builds the idempotent statement that copies base rows into the audit table at the given revision.
	 */
	String buildBackfillInsertSql(TableMapping mapping, List<String> dataColumns, List<String> joinColumns,
	        boolean hasRevType, int revId, String quote) {
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
		return sql.toString();
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
	
	String quoteIdentifier(String identifier, String quote) {
		if (quote.isEmpty()) {
			return identifier;
		}
		return quote + identifier.replace(quote, quote + quote) + quote;
	}
	
	private void safeRollback(Transaction tx) {
		try {
			if (tx != null && tx.isActive()) {
				tx.rollback();
			}
		}
		catch (RuntimeException e) {
			log.warn("Rollback failed during audit backfill: {}", e.getMessage());
		}
	}
	
	/** Resolved base/audit table pair for one audited entity. */
	public static final class TableMapping {
		
		private final String baseTable;
		
		private final String auditTable;
		
		public TableMapping(String baseTable, String auditTable) {
			this.baseTable = baseTable;
			this.auditTable = auditTable;
		}
		
		public String getBaseTable() {
			return baseTable;
		}
		
		public String getAuditTable() {
			return auditTable;
		}
	}
	
}
