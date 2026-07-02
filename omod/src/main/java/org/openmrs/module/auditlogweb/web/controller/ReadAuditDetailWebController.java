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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.List;

import static org.openmrs.module.auditlogweb.AuditlogwebConstants.MODULE_PATH;

@Controller("auditlogweb.ReadAuditDetailWeController")
@RequestMapping(value = MODULE_PATH + "/viewReadAudit.form")
@RequiredArgsConstructor
public class ReadAuditDetailWebController {
	
	private final Logger log = LoggerFactory.getLogger(ReadAuditDetailWebController.class);
	
	private static final String VIEW = MODULE_PATH + "/viewReadAuditLog";
	
	private final String ACCESS_DENIED_VIEW = MODULE_PATH + "/accessDenied";
	
	private static final int RELATED_EVENTS_LIMIT = 10;
	
	private final ReadAuditService readAuditService;
	
	@GetMapping
	public ModelAndView readAuditDetailWe(HttpServletRequest request, ModelMap model) {
		try {
			String logIdParam = request.getParameter("logId");
			
			if (logIdParam == null || logIdParam.isEmpty()) {
				model.addAttribute("errorMessage", "No read audit ID provided.");
				return new ModelAndView(VIEW, model);
			}
			
			Integer logId;
			try {
				logId = Integer.parseInt(logIdParam);
			}
			catch (NumberFormatException e) {
				model.addAttribute("errorMessage", "Invalid readAudit ID format.");
				return new ModelAndView(VIEW, model);
			}
			
			ReadAuditLog readAudit = readAuditService.getReadAuditLogById(logId);
			
			if (readAudit == null) {
				model.addAttribute("errorMessage", "Read audit not found.");
				return new ModelAndView(VIEW, model);
			}
			
			List<ReadAuditLog> relatedAudits = null;
			if (readAudit.getSessionId() != null && !readAudit.getSessionId().isEmpty()) {
				relatedAudits = readAuditService.getRelatedReadLogs(readAudit.getSessionId(), RELATED_EVENTS_LIMIT);
			}
			
			model.addAttribute("readAudit", readAudit);
			model.addAttribute("relatedAudits", relatedAudits != null ? relatedAudits : new ArrayList<>());
			
			return new ModelAndView(VIEW, model);
		}
		catch (APIAuthenticationException e) {
			return new ModelAndView(ACCESS_DENIED_VIEW, model);
		}
		catch (Exception e) {
			log.error("Error loading security audit detail: ", e);
			model.addAttribute("errorMessage", "Error loading audit data: " + e.getMessage());
			return new ModelAndView(VIEW, model);
		}
	}
}
