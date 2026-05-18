package com.verilearn.workflow.service;

import com.verilearn.progress.dto.ProgressResponse;
import com.verilearn.task.dto.TaskResponse;
import com.verilearn.chapter.dto.ChapterSummaryResponse;
import com.verilearn.chapter.dto.ChapterDemoEvaluationRequest;
import com.verilearn.chapter.dto.ChapterDemoEvaluationResponse;
import com.verilearn.workflow.dto.LearningRouteContentResponse;
import com.verilearn.workflow.dto.LearnerCurrentContextResponse;
import com.verilearn.workflow.dto.LearnerDashboardResponse;
import com.verilearn.workflow.dto.LearnerDemoSubmissionRequest;
import com.verilearn.workflow.dto.LearnerSetupRequest;
import com.verilearn.workflow.dto.LearnerSetupResponse;

import java.util.List;

public interface LearnerWorkflowService {

    LearnerSetupResponse setupLearner(LearnerSetupRequest request);

    TaskResponse generateTodayTask(String feishuOpenId);

    ProgressResponse getProgress(String feishuOpenId);

    List<ChapterSummaryResponse> listChapters(String feishuOpenId);

    LearnerDashboardResponse getDashboard(String feishuOpenId);

    LearnerCurrentContextResponse getCurrentContext(String feishuOpenId);

    LearningRouteContentResponse getLearningRoute(String feishuOpenId);

    String getLearningRouteViewHtml(String feishuOpenId);

    List<String> generateTopicOptions(String feishuOpenId, String topic);

    void clearLearningRoute(String feishuOpenId, String topic);

    ChapterDemoEvaluationResponse evaluateDemoSubmission(String feishuOpenId, Long chapterId, ChapterDemoEvaluationRequest request);

    ChapterDemoEvaluationResponse evaluateCurrentDemoSubmission(String feishuOpenId, LearnerDemoSubmissionRequest request);

    ChapterDemoEvaluationResponse evaluateCurrentDemoSubmission(String feishuOpenId, Long chapterId, LearnerDemoSubmissionRequest request);

    String completeReviews(String feishuOpenId, String chapterNoText);
}
