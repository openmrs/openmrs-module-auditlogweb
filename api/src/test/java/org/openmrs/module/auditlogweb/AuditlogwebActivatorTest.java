/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.auditlogweb;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuditlogwebActivatorTest {
	
	@InjectMocks
	private AuditlogwebActivator auditlogwebActivator;
	
	private AutoCloseable mocks;
	
	@BeforeEach
	void setUp() throws Exception {
		mocks = MockitoAnnotations.openMocks(this);
	}
	
	@AfterEach
	void tearDown() throws Exception {
		Field field = AuditlogwebActivator.class.getDeclaredField("appShuttingDown");
		field.setAccessible(true);
		field.setBoolean(null, false);
		
		if (mocks != null) {
			mocks.close();
		}
	}
	
	@Test
	void activatorStartShouldInitializeShutdownHookAndSetAppShuttingDownWhenRun() throws Exception {
		assertFalse(AuditlogwebActivator.isAppShuttingDown());
		
		auditlogwebActivator.started();
		
		Field field = AuditlogwebActivator.class.getDeclaredField("shutdownHook");
		field.setAccessible(true);
		Thread shutdownHook = (Thread) field.get(auditlogwebActivator);
		
		assertNotNull(shutdownHook);
		assertEquals("auditlogweb-shutdown-hook", shutdownHook.getName());
		
		shutdownHook.run();
		
		assertTrue(AuditlogwebActivator.isAppShuttingDown());
	}
	
	@Test
	void activatorStoppedShouldNullShutdownHook() throws Exception {
		auditlogwebActivator.started();
		
		Field field = AuditlogwebActivator.class.getDeclaredField("shutdownHook");
		field.setAccessible(true);
		assertNotNull(field.get(auditlogwebActivator));
		
		auditlogwebActivator.stopped();
		
		assertNull(field.get(auditlogwebActivator));
	}
}
