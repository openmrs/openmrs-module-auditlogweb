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
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.module.auditlogweb.api.dao.AuditBackfillDao;
import org.openmrs.module.auditlogweb.api.dao.AuditBackfillDao.TableMapping;
import org.openmrs.module.auditlogweb.api.utils.EnversUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Service class for the audit backfill
 */
@Component("auditlogweb.auditBackfillService")
@RequiredArgsConstructor
public class AuditBackfillService {
	
	private static final Logger log = LoggerFactory.getLogger(AuditBackfillService.class);
	
	public static final String GP_BACKFILL_ENABLED = "auditlogweb.backfillExistingData.enabled";
	
	public static final String GP_BACKFILL_COMPLETED = "auditlogweb.backfillExistingData.completed";
	
	public static final String GP_BACKFILL_REVISION = "auditlogweb.backfillExistingData.revision";
	
	private final AuditBackfillDao auditBackfillDao;
	
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
		
		List<TableMapping> mappings = auditBackfillDao.resolveAuditedTableMappings();
		if (mappings.isEmpty()) {
			log.warn("No audited entities resolved from the metamodel; aborting audit backfill.");
			return;
		}
		
		log.warn("Starting one-time audit backfill of existing data into {} audited tables...", mappings.size());
		
		Integer revId = reuseRevisionId();
		if (revId == null) {
			revId = auditBackfillDao.createBaselineRevision();
			administrationService.setGlobalProperty(GP_BACKFILL_REVISION, String.valueOf(revId));
		}
		int revision = revId;
		
		boolean allSucceeded = true;
		for (TableMapping mapping : auditBackfillDao.orderByAuditTableDependencies(mappings)) {
			try {
				long insertedRows = auditBackfillDao.backfillTable(mapping, revision);
				if (insertedRows > 0) {
					log.info("Audit backfill: {} -> {} ({} rows).", mapping.getBaseTable(), mapping.getAuditTable(),
					    insertedRows);
				}
			}
			catch (Exception e) {
				allSucceeded = false;
				log.warn("Audit backfill skipped for {} -> {}: {}", mapping.getBaseTable(), mapping.getAuditTable(),
				    describeRootCause(e));
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
	
	Integer reuseRevisionId() {
		String storedRevisionId = Context.getAdministrationService().getGlobalProperty(GP_BACKFILL_REVISION, "");
		if (StringUtils.isBlank(storedRevisionId)) {
			return null;
		}
		try {
			int revId = Integer.parseInt(storedRevisionId.trim());
			return auditBackfillDao.revisionExists(revId) ? revId : null;
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
	
	private String describeRootCause(Throwable t) {
		Throwable cause = t;
		while (cause.getCause() != null && cause.getCause() != cause) {
			cause = cause.getCause();
		}
		return cause.getClass().getSimpleName() + ": " + cause.getMessage();
	}
	
}
