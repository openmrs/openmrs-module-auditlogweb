package org.openmrs.module.auditlogweb.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

@Controller
public class DebugController {

    @RequestMapping(value = "/module/auditlogweb/simulateTimeout.form", method = RequestMethod.GET)
    @ResponseBody
    public String simulateTimeout(HttpServletRequest request) {

        try {
            HttpSession session = request.getSession(false);
            if (session == null) {
                return "No active session";
            }
            String sessionId = session.getId();
            session.invalidate();
            return "Session " + sessionId + " invalidated — check logs for SESSION_TIMEOUT";
        }
        catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}
