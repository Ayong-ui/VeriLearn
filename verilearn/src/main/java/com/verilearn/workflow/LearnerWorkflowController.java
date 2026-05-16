package com.verilearn.workflow;

import com.verilearn.chapter.dto.ChapterSummaryResponse;
import com.verilearn.chapter.dto.ChapterDemoEvaluationRequest;
import com.verilearn.chapter.dto.ChapterDemoEvaluationResponse;
import com.verilearn.common.ApiResponse;
import com.verilearn.progress.dto.ProgressResponse;
import com.verilearn.task.dto.TaskResponse;
import com.verilearn.workflow.dto.LearningRouteContentResponse;
import com.verilearn.workflow.dto.LearnerCurrentContextResponse;
import com.verilearn.workflow.dto.LearnerDashboardResponse;
import com.verilearn.workflow.dto.LearnerDemoSubmissionRequest;
import com.verilearn.workflow.dto.LearnerSetupRequest;
import com.verilearn.workflow.dto.LearnerSetupResponse;
import com.verilearn.workflow.service.LearnerWorkflowService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/learners")
public class LearnerWorkflowController {

    private final LearnerWorkflowService learnerWorkflowService;

    public LearnerWorkflowController(LearnerWorkflowService learnerWorkflowService) {
        this.learnerWorkflowService = learnerWorkflowService;
    }

    @PostMapping("/setup")
    public ApiResponse<LearnerSetupResponse> setupLearner(@Valid @RequestBody LearnerSetupRequest request) {
        return ApiResponse.success("learner setup completed successfully", learnerWorkflowService.setupLearner(request));
    }

    @GetMapping("/{feishuOpenId}/today-task")
    public ApiResponse<TaskResponse> generateTodayTask(@PathVariable String feishuOpenId) {
        return ApiResponse.success("today task generated successfully", learnerWorkflowService.generateTodayTask(feishuOpenId));
    }

    @GetMapping("/{feishuOpenId}/progress")
    public ApiResponse<ProgressResponse> getProgress(@PathVariable String feishuOpenId) {
        return ApiResponse.success("progress queried successfully", learnerWorkflowService.getProgress(feishuOpenId));
    }

    @GetMapping("/{feishuOpenId}/chapters")
    public ApiResponse<List<ChapterSummaryResponse>> listChapters(@PathVariable String feishuOpenId) {
        return ApiResponse.success("chapters queried successfully", learnerWorkflowService.listChapters(feishuOpenId));
    }

    @GetMapping("/{feishuOpenId}/dashboard")
    public ApiResponse<LearnerDashboardResponse> getDashboard(@PathVariable String feishuOpenId) {
        return ApiResponse.success("dashboard queried successfully", learnerWorkflowService.getDashboard(feishuOpenId));
    }

    @GetMapping("/{feishuOpenId}/current-context")
    public ApiResponse<LearnerCurrentContextResponse> getCurrentContext(@PathVariable String feishuOpenId) {
        return ApiResponse.success("current learning context queried successfully", learnerWorkflowService.getCurrentContext(feishuOpenId));
    }

    @GetMapping("/{feishuOpenId}/learning-route")
    public ApiResponse<LearningRouteContentResponse> getLearningRoute(@PathVariable String feishuOpenId) {
        return ApiResponse.success("learning route queried successfully", learnerWorkflowService.getLearningRoute(feishuOpenId));
    }

    @PostMapping("/{feishuOpenId}/chapters/{chapterId}/demo-evaluations")
    public ApiResponse<ChapterDemoEvaluationResponse> evaluateDemoSubmission(
            @PathVariable String feishuOpenId,
            @PathVariable Long chapterId,
            @Valid @RequestBody ChapterDemoEvaluationRequest request
    ) {
        return ApiResponse.success(
                "learner demo evaluated successfully",
                learnerWorkflowService.evaluateDemoSubmission(feishuOpenId, chapterId, request)
        );
    }

    @PostMapping("/{feishuOpenId}/chapters/{chapterId}/demo-feedback")
    public ApiResponse<ChapterDemoEvaluationResponse> evaluateCurrentDemoSubmission(
            @PathVariable String feishuOpenId,
            @PathVariable Long chapterId,
            @Valid @RequestBody LearnerDemoSubmissionRequest request
    ) {
        return ApiResponse.success(
                "learner current demo evaluated successfully",
                learnerWorkflowService.evaluateCurrentDemoSubmission(feishuOpenId, chapterId, request)
        );
    }

    @PostMapping("/{feishuOpenId}/demo-feedback/current")
    public ApiResponse<ChapterDemoEvaluationResponse> evaluateCurrentDemoSubmission(
            @PathVariable String feishuOpenId,
            @Valid @RequestBody LearnerDemoSubmissionRequest request
    ) {
        return ApiResponse.success(
                "learner current demo evaluated successfully",
                learnerWorkflowService.evaluateCurrentDemoSubmission(feishuOpenId, request)
        );
    }
}
