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
import org.openmrs.module.auditlogweb.ReadAuditLog;
import org.openmrs.module.auditlogweb.api.ReadAuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import org.openmrs.module.auditlogweb.api.utils.UtilClass;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Collections;

import static org.openmrs.module.auditlogweb.AuditlogwebConstants.MODULE_PATH;

@Controller("auditlogweb.ReadAuditWebController")
@RequestMapping(value = MODULE_PATH + "/readauditlogs.form")
@RequiredArgsConstructor
public class ReadAuditWebController {
	
	private final Logger log = LoggerFactory.getLogger(ReadAuditWebController.class);
	
	private static final String VIEW = MODULE_PATH + "/readAuditLogs";
	
	private final String ACCESS_DENIED_VIEW = MODULE_PATH + "/accessDenied";
	
	private final ReadAuditService readAuditService;
	
	@RequestMapping(method = RequestMethod.GET)
	public ModelAndView readAudit(@RequestParam(value = "entityType", required = false) String entityType,
	        @RequestParam(value = "username", required = false) String username,
	        @RequestParam(value = "startDate", required = false) String startDate,
	        @RequestParam(value = "endDate", required = false) String endDate,
	        @RequestParam(value = "page", defaultValue = "0") int page,
	        @RequestParam(value = "size", defaultValue = "15") int size, ModelMap model) {
		
		Date start = UtilClass.parseDate(startDate, false);
		Date end = UtilClass.parseDate(endDate, true);
		
		try {
			List<ReadAuditLog> readAuditLogs = readAuditService.getReadAuditLogs(entityType, username, start, end, page,
			    size);
			long totalCount = readAuditService.countReadAuditLogs(entityType, username, start, end);
			int totalPages = (int) Math.ceil((double) totalCount / Math.max(size, 1));
			
			model.addAttribute("readAuditLogs", readAuditLogs);
			model.addAttribute("totalCount", totalCount);
			model.addAttribute("totalPages", totalPages);
			model.addAttribute("hasNextPage", page + 1 < totalPages);
			model.addAttribute("hasPreviousPage", page > 0);
			model.addAttribute("currentPage", page);
			model.addAttribute("pageSize", size);
			model.addAttribute("eventType", entityType);
			model.addAttribute("usernameFilter", username);
			model.addAttribute("startDate", startDate);
			model.addAttribute("endDate", endDate);
			model.addAttribute("page", "readauditlogs");
			model.addAttribute("entityTypes", getEntityTypes());
		}
		catch (APIAuthenticationException e) {
			return new ModelAndView(ACCESS_DENIED_VIEW, model);
		}
		catch (Exception e) {
			log.error("Failed to load security audit logs", e);
			model.addAttribute("errorMessage", "An error occurred while loading security audit logs.");
			model.addAttribute("readAuditLogs", Collections.emptyList());
			
		}
		return new ModelAndView(VIEW);
	}
	
	private List<String> getEntityTypes() {
		return Arrays.asList("PATIENT");
	}
}
