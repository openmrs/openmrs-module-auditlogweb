/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.auditlogweb.web.controller;

import lombok.RequiredArgsConstructor;
import org.openmrs.api.APIAuthenticationException;
import org.openmrs.module.auditlogweb.ModuleEvent;
import org.openmrs.module.auditlogweb.api.ModuleEventService;
import org.openmrs.module.auditlogweb.api.utils.ModuleEventType;
import org.openmrs.module.auditlogweb.api.utils.UtilClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.openmrs.module.auditlogweb.AuditlogwebConstants.MODULE_PATH;

@Controller("auditlogweb.ModuleEventController")
@RequestMapping(value = MODULE_PATH + "/moduleEvents.form")
@RequiredArgsConstructor
public class ModuleEventWebController {
	
	private final Logger log = LoggerFactory.getLogger(ModuleEventWebController.class);
	
	private static final String VIEW = MODULE_PATH + "/moduleEvents";
	
	private final String ACCESS_DENIED_VIEW = MODULE_PATH + "/accessDenied";
	
	private final ModuleEventService moduleEventService;
	
	@GetMapping()
	public String onView(@RequestParam(value = "eventType", required = false) String eventType,
	        @RequestParam(value = "moduleName", required = false) String moduleName,
	        @RequestParam(value = "username", required = false) String username,
	        @RequestParam(value = "userUUID", required = false) String userUUID,
	        @RequestParam(value = "startDate", required = false) String startDate,
	        @RequestParam(value = "endDate", required = false) String endDate,
	        @RequestParam(value = "page", defaultValue = "0") int page,
	        @RequestParam(value = "size", defaultValue = "15") int size, Model model) {
		
		Date start = UtilClass.toStartDate(UtilClass.parse(startDate));
		Date end = UtilClass.toEndDate(UtilClass.parse(endDate));
		
		try {
			
			List<ModuleEvent> events = moduleEventService.getModuleEvents(eventType, null, moduleName, null, username,
			    userUUID, start, end, page, size);
			long totalCount = moduleEventService.countModuleEvents(eventType, null, moduleName, null, username, userUUID,
			    start, end);
			int totalPages = UtilClass.computeTotalPages(totalCount, size);
			
			model.addAttribute("events", events);
			model.addAttribute("totalCount", totalCount);
			model.addAttribute("totalPages", totalPages);
			model.addAttribute("hasNextPage", page + 1 < totalPages);
			model.addAttribute("hasPreviousPage", page > 0);
			model.addAttribute("currentPage", page);
			model.addAttribute("pageSize", size);
			model.addAttribute("eventType", eventType);
			model.addAttribute("usernameFilter", username);
			model.addAttribute("startDate", startDate);
			model.addAttribute("endDate", endDate);
			model.addAttribute("page", "moduleventlogs");
			model.addAttribute("eventTypes", ModuleEventType.values());
		}
		catch (APIAuthenticationException e) {
			return ACCESS_DENIED_VIEW;
		}
		catch (Exception e) {
			log.error("Failed to load module audit logs", e);
			model.addAttribute("errorMessage", "An error occurred while loading module audit logs.");
			model.addAttribute("events", Arrays.asList());
			model.addAttribute("eventTypes", ModuleEventType.values());
			model.addAttribute("page", "moduleventlogs");
		}
		
		return VIEW;
	}
	
}
