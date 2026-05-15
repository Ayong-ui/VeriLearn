package com.verilearn.chapter.service;

import com.verilearn.chapter.dto.ChapterBootstrapResponse;
import com.verilearn.chapter.dto.ChapterDetailResponse;
import com.verilearn.chapter.dto.ChapterDemoEvaluationRequest;
import com.verilearn.chapter.dto.ChapterDemoEvaluationResponse;
import com.verilearn.chapter.dto.ChapterMaterialContentResponse;
import com.verilearn.chapter.dto.ChapterStepSubmitRequest;
import com.verilearn.chapter.dto.ChapterStepSubmitResponse;
import com.verilearn.chapter.dto.ChapterSummaryResponse;

import java.util.List;

public interface ChapterService {

    ChapterBootstrapResponse bootstrapChapters(Long goalId);

    List<ChapterSummaryResponse> listChaptersByGoalId(Long goalId);

    ChapterDetailResponse getChapterDetail(Long chapterId);

    ChapterDetailResponse startChapter(Long chapterId);

    ChapterStepSubmitResponse submitStep(Long chapterId, ChapterStepSubmitRequest request);

    ChapterDetailResponse completeReview(Long chapterId);

    List<ChapterSummaryResponse> listPendingReviewsByGoalId(Long goalId);

    ChapterDetailResponse generateMaterials(Long chapterId);

    ChapterMaterialContentResponse getMaterialContent(Long materialId);

    String getMaterialViewHtml(Long materialId);

    ChapterDemoEvaluationResponse evaluateDemoSubmission(Long chapterId, ChapterDemoEvaluationRequest request);
}
