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

import lombok.RequiredArgsConstructor;
import org.openmrs.api.context.Daemon;
import org.openmrs.module.AbstractModuleEvent;
import org.openmrs.module.ModuleLoadEvent;
import org.openmrs.module.ModuleStartEvent;
import org.openmrs.module.ModuleStopEvent;
import org.openmrs.module.ModuleUnloadEvent;
import org.openmrs.module.auditlogweb.api.utils.ModuleEventType;
import org.openmrs.module.auditlogweb.AuditlogwebActivator;
import org.openmrs.module.auditlogweb.api.AuditLogRecorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ModuleEventListener {
	
	private final Logger log = LoggerFactory.getLogger(ModuleEventListener.class);
	
	private final AuditLogRecorder auditLogRecorder;
	
	/**
	 * Listening to the module event like {@link ModuleLoadEvent}, {@link ModuleStartEvent},
	 * {@link ModuleStopEvent}, {@link ModuleUnloadEvent} which got published from the core.We do not
	 * need the module action during the startup and shutdown of application that's why ignoring them.
	 */
	@EventListener
	public void listenModuleAction(AbstractModuleEvent moduleActionEvent) {
		try {
			if (Daemon.isDaemonThread()) {
				return;
			}
			
			if (AuditlogwebActivator.isAppShuttingDown()) {
				return;
			}
			
			ModuleEventType moduleEventType;
			if (moduleActionEvent instanceof ModuleLoadEvent) {
				moduleEventType = ModuleEventType.MODULE_LOAD;
			} else if (moduleActionEvent instanceof ModuleStartEvent) {
				moduleEventType = ModuleEventType.MODULE_START;
			} else if (moduleActionEvent instanceof ModuleStopEvent) {
				moduleEventType = ModuleEventType.MODULE_STOP;
			} else {
				moduleEventType = ModuleEventType.MODULE_UNLOAD;
			}
			
			String moduleId = moduleActionEvent.getModuleId();
			String moduleName = moduleActionEvent.getModuleName();
			String moduleVersion = moduleActionEvent.getModuleVersion();
			boolean isSuccess = moduleActionEvent.isSuccess();
			String failureReason = moduleActionEvent.getFailureReason();
			
			auditLogRecorder.logModuleEvent(moduleEventType, moduleId, moduleName, moduleVersion, isSuccess, failureReason);
		}
		catch (Exception e) {
			log.error("Error occur while listening to module events", e);
		}
	}
}
