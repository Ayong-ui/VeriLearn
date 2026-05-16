package com.verilearn.workflow;

import com.verilearn.workflow.service.LearnerWorkflowService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LearningRoutePageController {

    private final LearnerWorkflowService learnerWorkflowService;

    public LearningRoutePageController(LearnerWorkflowService learnerWorkflowService) {
        this.learnerWorkflowService = learnerWorkflowService;
    }

    @GetMapping(value = "/learning-routes/{feishuOpenId}/view", produces = MediaType.TEXT_HTML_VALUE)
    public String viewLearningRoute(@PathVariable String feishuOpenId) {
        return learnerWorkflowService.getLearningRouteViewHtml(feishuOpenId);
    }
}
