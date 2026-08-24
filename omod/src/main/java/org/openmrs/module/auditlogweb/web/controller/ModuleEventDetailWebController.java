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

@Controller("auditlogweb.ModuleEventDetailController")
@RequestMapping(value = MODULE_PATH + "/viewModuleEvent.form")
@RequiredArgsConstructor
public class ModuleEventDetailWebController {
	
	private static final Logger log = LoggerFactory.getLogger(ModuleEventDetailWebController.class);
	
	private final ModuleEventService moduleEventService;
	
	private static final String VIEW = MODULE_PATH + "/viewModuleEvent";
	
	private final String ACCESS_DENIED_VIEW = MODULE_PATH + "/accessDenied";
	
	private static final int RELATED_EVENTS_LIMIT = 10;
	
	@GetMapping
	public ModelAndView showDetails(HttpServletRequest request, ModelMap model) {
		
		try {
			String eventIdParam = request.getParameter("eventId");
			
			if (eventIdParam == null || eventIdParam.isEmpty()) {
				model.addAttribute("errorMessage", "No event ID provided.");
				return new ModelAndView(VIEW, model);
			}
			
			Integer eventId;
			try {
				eventId = Integer.parseInt(eventIdParam);
			}
			catch (NumberFormatException e) {
				model.addAttribute("errorMessage", "Invalid event ID format.");
				return new ModelAndView(VIEW, model);
			}
			
			ModuleEvent event = moduleEventService.getModuleEventById(eventId);
			
			if (event == null) {
				model.addAttribute("errorMessage", "Module event log not found.");
				return new ModelAndView(VIEW, model);
			}
			
			List<ModuleEvent> relatedEvents = new ArrayList<>();
			if (event.getSessionId() != null && !event.getSessionId().isEmpty()) {
				relatedEvents = moduleEventService.getRelatedModuleEvents(event.getSessionId(), 0, RELATED_EVENTS_LIMIT);
			}
			
			model.addAttribute("event", event);
			model.addAttribute("relatedEvents", relatedEvents);
			
			return new ModelAndView(VIEW, model);
		}
		catch (APIAuthenticationException e) {
			return new ModelAndView(ACCESS_DENIED_VIEW, model);
		}
		catch (Exception e) {
			log.error("Error loading read audit detail: ", e);
			model.addAttribute("errorMessage", "Error loading audit data: " + e.getMessage());
			return new ModelAndView(VIEW, model);
		}
	}
	
}
