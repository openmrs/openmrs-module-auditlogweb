/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.auditlogweb.web.controller;

import java.time.LocalDate;
import org.apache.commons.lang.StringUtils;
import org.openmrs.module.auditlogweb.api.AuditService;
import org.openmrs.module.auditlogweb.api.utils.UtilClass;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * This class configured as controller using annotation and mapped with the URL of
 * 'module/${rootArtifactid}/${rootArtifactid}Link.form'.
 */
@Controller
@RequestMapping(value = "module/auditlogweb/auditlogs")
public class AuditlogwebController {

	private final String VIEW = "auditlogs";

	private final AuditService auditService;

    public AuditlogwebController(AuditService auditService) {
        this.auditService = auditService;
    }
	@GetMapping
	public String showAuditLogs(
			@RequestParam(value = "selectedClass", required = false) String domainName,
			@RequestParam(value = "username", required = false) String username,
			@RequestParam(value = "startDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
			@RequestParam(value = "endDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
			Model model) {

		model.addAttribute("classes", UtilClass.findClassesWithAnnotation());

		if (StringUtils.isNotBlank(domainName)) {
			model.addAttribute("audits", auditService.getAllRevisions(domainName));
			model.addAttribute("currentClass", domainName);
		}

		model.addAttribute("username", username);
		model.addAttribute("startDate", startDate);
		model.addAttribute("endDate", endDate);

		return VIEW;
	}
}
