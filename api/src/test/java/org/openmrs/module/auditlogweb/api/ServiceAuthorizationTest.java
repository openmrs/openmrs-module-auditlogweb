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

import org.junit.jupiter.api.Test;
import org.openmrs.api.APIAuthenticationException;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.UserContext;
import org.openmrs.module.auditlogweb.api.utils.AuditLogConstants;
import org.openmrs.test.jupiter.BaseContextSensitiveTest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ServiceAuthorizationTest extends BaseContextSensitiveTest {
	
	@Test
	void shouldRejectAuditServiceCallsWhenUserLacksViewAuditLogsPrivilege() {
		UserContext userContext = mock(UserContext.class);
		when(userContext.hasPrivilege(AuditLogConstants.VIEW_AUDIT_LOGS)).thenReturn(false);
		when(userContext.isAuthenticated()).thenReturn(true);
		
		contextMockHelper.setUserContext(userContext);
		
		assertFalse(Context.hasPrivilege(AuditLogConstants.VIEW_AUDIT_LOGS));
		assertThrows(APIAuthenticationException.class, () -> Context.getService(AuditService.class).getAuditLogsCount());
	}
	
	@Test
	void shouldRejectAuditServiceCallsWhenUserLacksViewSecurityAuditLogsPrivilege() {
		UserContext userContext = mock(UserContext.class);
		when(userContext.hasPrivilege(AuditLogConstants.VIEW_SECURITY_AUDIT_LOGS)).thenReturn(false);
		when(userContext.isAuthenticated()).thenReturn(true);
		
		contextMockHelper.setUserContext(userContext);
		
		assertFalse(Context.hasPrivilege(AuditLogConstants.VIEW_SECURITY_AUDIT_LOGS));
		assertThrows(APIAuthenticationException.class, () -> Context.getService(AuditService.class).getSecurityEventById(1));
	}
}
