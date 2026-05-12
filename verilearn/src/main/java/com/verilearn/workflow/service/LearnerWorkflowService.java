package com.verilearn.workflow.service;

import com.verilearn.progress.dto.ProgressResponse;
import com.verilearn.task.dto.TaskResponse;
import com.verilearn.chapter.dto.ChapterSummaryResponse;
import com.verilearn.workflow.dto.LearnerCurrentContextResponse;
import com.verilearn.workflow.dto.LearnerDashboardResponse;
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
}
