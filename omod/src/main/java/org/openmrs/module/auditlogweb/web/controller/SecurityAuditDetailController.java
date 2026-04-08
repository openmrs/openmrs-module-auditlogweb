/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.auditlogweb.web.controller;

import lombok.RequiredArgsConstructor;
import org.openmrs.module.auditlogweb.AuditSecurityEvent;
import org.openmrs.module.auditlogweb.api.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

import static org.openmrs.module.auditlogweb.AuditlogwebConstants.MODULE_PATH;

/**
 * Controller for displaying security audit event details.
 * Maps to /module/auditlogweb/viewSecurityAudit.form?eventId={id}
 * Displays the event details and related activity from the same session.
 */
@Controller("auditlogweb.SecurityAuditDetailController")
@RequestMapping(value = MODULE_PATH + "/viewSecurityAudit.form")
@RequiredArgsConstructor
public class SecurityAuditDetailController {

    private static final Logger logger = LoggerFactory.getLogger(SecurityAuditDetailController.class);

    private final AuditService auditService;

    private static final String VIEW = MODULE_PATH + "/viewSecurityAudit";
    private static final int RELATED_EVENTS_LIMIT = 10;

    /**
     * Display security audit event details.
     * 
     * @param request HTTP request
     * @param model   model map for JSP
     * @return ModelAndView pointing to viewSecurityAudit.jsp
     */
    @RequestMapping(method = RequestMethod.GET)
    public ModelAndView showDetails(HttpServletRequest request, ModelMap model) {
        try {
            String eventIdParam = request.getParameter("eventId");

            if (eventIdParam == null || eventIdParam.isEmpty()) {
                model.addAttribute("errorMessage", "No event ID provided.");
                return new ModelAndView(VIEW, model);
            }

            Long eventId;
            try {
                eventId = Long.parseLong(eventIdParam);
            } catch (NumberFormatException e) {
                model.addAttribute("errorMessage", "Invalid event ID format.");
                return new ModelAndView(VIEW, model);
            }

            AuditSecurityEvent event = auditService.getSecurityEventById(eventId);

            if (event == null) {
                model.addAttribute("errorMessage", "Security event not found.");
                return new ModelAndView(VIEW, model);
            }

            // Fetch related events from the same session
            List<AuditSecurityEvent> relatedEvents = null;
            if (event.getSessionId() != null && !event.getSessionId().isEmpty()) {
                relatedEvents = auditService.getRelatedSecurityEvents(event.getSessionId(), RELATED_EVENTS_LIMIT);
            }

            model.addAttribute("event", event);
            model.addAttribute("relatedEvents", relatedEvents != null ? relatedEvents : new java.util.ArrayList<>());
            model.addAttribute("page", "securityauditlogs");

            return new ModelAndView(VIEW, model);

        } catch (Exception e) {
            logger.error("Error loading security audit detail: ", e);
            model.addAttribute("errorMessage", "Error loading audit data: " + e.getMessage());
            return new ModelAndView(VIEW, model);
        }
    }
}
