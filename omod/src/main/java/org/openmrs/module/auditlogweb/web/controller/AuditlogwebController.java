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
import org.openmrs.module.auditlogweb.api.utils.EnversUtils;
import org.openmrs.module.auditlogweb.api.utils.UtilClass;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;

/**
 * This class configured as controller using annotation and mapped with the URL of
 * 'module/${rootArtifactid}/${rootArtifactid}Link.form'.
 */
@Controller
@RequestMapping(value = AuditlogwebController.MODULE_PATH + "/auditlogs.form")
@RequiredArgsConstructor
public class AuditlogwebController {

    public static final String MODULE_PATH = "/module/auditlogweb";
    private final String VIEW = MODULE_PATH + "/auditlogs";
    private final String ENVERS_DISABLED_VIEW = MODULE_PATH + "/enversDisabled";

    private final AuditService auditService;

    @RequestMapping(method = RequestMethod.GET)
    public String onGet() {
        return VIEW;
    }

    @ModelAttribute("classes")
    protected List<String> getClasses() throws Exception {
        return UtilClass.findClassesWithAnnotation();
    }

    @RequestMapping(method = RequestMethod.POST)
    public String showClassFormAndAudits(@RequestParam(value = "selectedClass", required = false) String domainName, Model model) {
        // check if envers is enable
        if (!EnversUtils.isEnversEnabled()) {
            model.addAttribute("errorMessage", "Audit logging is not enabled on this server. " + "Please enable Hibernate Envers in openmrs-runtime.properties to view audit logs.");
            return ENVERS_DISABLED_VIEW;
        }
        if (domainName != null && !domainName.isEmpty()) {
            model.addAttribute("audits", auditService.getAllRevisions(domainName));
            model.addAttribute("currentClass", domainName);
        }
        return VIEW;
    }

}
