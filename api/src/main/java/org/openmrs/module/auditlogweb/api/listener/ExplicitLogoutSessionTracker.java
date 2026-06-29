/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.auditlogweb.api.listener;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks sessions that were destroyed after an explicit logout, so destruction is not audited as a
 * session timeout.
 */
public final class ExplicitLogoutSessionTracker {
	
	private static final Set<String> SESSION_IDS = ConcurrentHashMap.newKeySet();
	
	private ExplicitLogoutSessionTracker() {
	}
	
	public static void mark(String sessionId) {
		if (sessionId != null) {
			SESSION_IDS.add(sessionId);
		}
	}
	
	public static boolean consume(String sessionId) {
		return sessionId != null && SESSION_IDS.remove(sessionId);
	}
}
