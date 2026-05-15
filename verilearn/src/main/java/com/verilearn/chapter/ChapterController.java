package com.verilearn.chapter;

import com.verilearn.chapter.dto.ChapterBootstrapResponse;
import com.verilearn.chapter.dto.ChapterDetailResponse;
import com.verilearn.chapter.dto.ChapterDemoEvaluationRequest;
import com.verilearn.chapter.dto.ChapterDemoEvaluationResponse;
import com.verilearn.chapter.dto.ChapterMaterialContentResponse;
import com.verilearn.chapter.dto.ChapterStepSubmitRequest;
import com.verilearn.chapter.dto.ChapterStepSubmitResponse;
import com.verilearn.chapter.dto.ChapterSummaryResponse;
import com.verilearn.chapter.service.ChapterService;
import com.verilearn.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ChapterController {

    private final ChapterService chapterService;

    public ChapterController(ChapterService chapterService) {
        this.chapterService = chapterService;
    }

    @PostMapping("/api/goals/{goalId}/chapters/bootstrap")
    public ApiResponse<ChapterBootstrapResponse> bootstrapChapters(@PathVariable Long goalId) {
        return ApiResponse.success("chapters bootstrapped successfully", chapterService.bootstrapChapters(goalId));
    }

    @GetMapping("/api/goals/{goalId}/chapters")
    public ApiResponse<List<ChapterSummaryResponse>> listChapters(@PathVariable Long goalId) {
        return ApiResponse.success("chapters queried successfully", chapterService.listChaptersByGoalId(goalId));
    }

    @GetMapping("/api/goals/{goalId}/chapters/reviews/pending")
    public ApiResponse<List<ChapterSummaryResponse>> listPendingReviews(@PathVariable Long goalId) {
        return ApiResponse.success("pending chapter reviews queried successfully", chapterService.listPendingReviewsByGoalId(goalId));
    }

    @GetMapping("/api/chapters/{chapterId}")
    public ApiResponse<ChapterDetailResponse> getChapterDetail(@PathVariable Long chapterId) {
        return ApiResponse.success("chapter detail queried successfully", chapterService.getChapterDetail(chapterId));
    }

    @PostMapping("/api/chapters/{chapterId}/start")
    public ApiResponse<ChapterDetailResponse> startChapter(@PathVariable Long chapterId) {
        return ApiResponse.success("chapter started successfully", chapterService.startChapter(chapterId));
    }

    @PostMapping("/api/chapters/{chapterId}/steps/submit")
    public ApiResponse<ChapterStepSubmitResponse> submitStep(
            @PathVariable Long chapterId,
            @Valid @RequestBody ChapterStepSubmitRequest request
    ) {
        return ApiResponse.success("chapter step submitted successfully", chapterService.submitStep(chapterId, request));
    }

    @PostMapping("/api/chapters/{chapterId}/review/complete")
    public ApiResponse<ChapterDetailResponse> completeReview(@PathVariable Long chapterId) {
        return ApiResponse.success("chapter review completed successfully", chapterService.completeReview(chapterId));
    }

    @PostMapping("/api/chapters/{chapterId}/materials/generate")
    public ApiResponse<ChapterDetailResponse> generateMaterials(@PathVariable Long chapterId) {
        return ApiResponse.success("chapter materials generated successfully", chapterService.generateMaterials(chapterId));
    }

    @GetMapping("/api/materials/{materialId}/content")
    public ApiResponse<ChapterMaterialContentResponse> getMaterialContent(@PathVariable Long materialId) {
        return ApiResponse.success("chapter material content queried successfully", chapterService.getMaterialContent(materialId));
    }

    @GetMapping(value = "/materials/{materialId}/view", produces = MediaType.TEXT_HTML_VALUE)
    public String viewMaterial(@PathVariable Long materialId) {
        return chapterService.getMaterialViewHtml(materialId);
    }

    @PostMapping("/api/chapters/{chapterId}/demo-evaluations")
    public ApiResponse<ChapterDemoEvaluationResponse> evaluateDemoSubmission(
            @PathVariable Long chapterId,
            @Valid @RequestBody ChapterDemoEvaluationRequest request
    ) {
        return ApiResponse.success("chapter demo evaluated successfully", chapterService.evaluateDemoSubmission(chapterId, request));
    }
}
