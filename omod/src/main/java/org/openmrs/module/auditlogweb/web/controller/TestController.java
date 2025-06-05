package org.openmrs.module.auditlogweb.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("module/auditlogweb/test")
public class TestController {

    @GetMapping
    public String test(Model model) {
        model.addAttribute("message", "Hello from testing module");
        return "test";
    }
}
