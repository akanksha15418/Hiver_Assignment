package com.hiver.ai;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/support")
public class SupportController {

    @Autowired
    private SupportAgentService supportAgentService;

    @PostMapping("/handle")
    public SupportResponse handleSupportRequest(@RequestBody SupportRequest request) {
        return supportAgentService.handleSupportRequest(request.getMessage());
    }
}
