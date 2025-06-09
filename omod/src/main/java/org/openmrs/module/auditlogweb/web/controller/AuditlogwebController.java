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
import org.openmrs.module.auditlogweb.api.AuditService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * This class configured as controller using annotation and mapped with the URL of
 * 'module/${rootArtifactid}/${rootArtifactid}Link.form'.
 */
@Controller
@RequestMapping(value = "module/auditlogweb/auditlogs.form")
@RequiredArgsConstructor
public class AuditlogwebController {

	private final AuditService auditService;
	/** Success form view name */
	private final String VIEW = "/module/auditlogweb/auditlogs";


	@RequestMapping(method = RequestMethod.GET)
	public String onGet() {
		return VIEW;
	}

	public String showClassFormAndAudits(@RequestParam(required = false) String className, Model model) {
		if (className != null && !className.isEmpty()) {
			model.addAttribute("audits", auditService.getAllRevisions(className));
			model.addAttribute("currentClass", className);
		}
		return VIEW;
	}
	
}
