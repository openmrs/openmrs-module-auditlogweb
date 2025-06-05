package org.openmrs.module.auditlogweb.web.controller;

import org.openmrs.module.auditlogweb.api.AuditService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping(value = "module/auditlogweb/viewAudit")
public class ViewAuditController {

    private final String VIEW = "viewAudit";
    private final AuditService auditService;

    public ViewAuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    public String showAuditDetails(
            @RequestParam("auditId") int auditId,
            @RequestParam("entityId") int entityId,
            @RequestParam("class") String className,
            Model model) {
        try {
            Class<?> clazz = Class.forName(className);
            Object currentEntity = auditService.getRevisionById(clazz, entityId, auditId);

            if (auditId - 1 > 0) {
                Object oldEntity = auditService.getRevisionById(clazz, entityId, --auditId);
                model.addAttribute("oldEntity", oldEntity);
            }

            model.addAttribute("currentEntity", currentEntity);
            model.addAttribute("entityClass", clazz);

        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Class not found: " + className, e);
        }

        return VIEW;
    }
}