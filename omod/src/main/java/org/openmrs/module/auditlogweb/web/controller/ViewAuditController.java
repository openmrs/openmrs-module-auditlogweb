package org.openmrs.module.auditlogweb.web.controller;

import org.openmrs.module.auditlogweb.api.AuditlogwebService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("module/auditlogweb/viewAudit.form")
public class ViewAuditController {
	
	private final String VIEW_AUDIT = "/module/auditlogweb/viewAudit";
	
	private final AuditlogwebService auditlogwebService;
	
	public ViewAuditController(AuditlogwebService auditlogwebService) {
		this.auditlogwebService = auditlogwebService;
	}
	
	@RequestMapping(method = RequestMethod.GET)
	public ModelAndView showForm(HttpServletRequest request, ModelMap model) {
		int auditId = Integer.parseInt(request.getParameter("auditId"));
		int entityId = Integer.parseInt(request.getParameter("entityId"));
		Class clazz;
		try {
			clazz = Class.forName(request.getParameter("class"));
		}
		catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
		
		Object currentEntity = auditlogwebService.getAllRevisionById(clazz, entityId, auditId);
		if (auditId - 1 > 0) {
			Object oldEntity = auditlogwebService.getAllRevisionById(clazz, entityId, auditId - 1);
			model.addAttribute("oldEntity", clazz.cast(oldEntity));
		}
		model.addAttribute("currentEntity", clazz.cast(currentEntity));
		return new ModelAndView(VIEW_AUDIT, model);
	}
}
