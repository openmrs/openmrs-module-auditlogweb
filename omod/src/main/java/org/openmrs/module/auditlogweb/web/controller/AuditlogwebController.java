/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.auditlogweb.web.controller;

import java.util.List;
import org.openmrs.module.auditlogweb.api.AuditlogwebService;
import org.openmrs.module.auditlogweb.api.utill.ClassUtil;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * This class configured as controller using annotation and mapped with the URL of
 * 'module/${rootArtifactid}/${rootArtifactid}Link.form'.
 */
@Controller
@RequestMapping(value = "module/auditlogweb/allAudits.form")
public class AuditlogwebController {
	
	/** Success form view name */
	private final String VIEW = "/module/auditlogweb/allAudits";
	
	private final AuditlogwebService auditlogwebService;
	
	/**
	 * Default constructor
	 */
	public AuditlogwebController(AuditlogwebService auditlogwebService) {
		this.auditlogwebService = auditlogwebService;
	}
	
	/**
	 * Initially called after the getUsers method to get the landing form name
	 * 
	 * @return String form view name
	 */
	@RequestMapping(method = RequestMethod.GET)
	public String onGet() {
		return VIEW;
	}
	
	@ModelAttribute("classes")
	protected List<String> getClasses() throws Exception {
		return ClassUtil.findClassesWithAuditedAnnotation();
	}
	
	@RequestMapping(method = RequestMethod.POST)
	public String showClassFormAndAudits(@RequestParam(value = "selectedClass", required = false) String className,
	        Model model) {
		if (className != null && !className.isEmpty()) {
			model.addAttribute("audits", auditlogwebService.getAllRevisions(className));
			model.addAttribute("currentClass", className);
		}
		return VIEW;
	}
	
}
