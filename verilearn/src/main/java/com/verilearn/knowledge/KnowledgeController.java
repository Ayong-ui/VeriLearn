package com.verilearn.knowledge;

import com.verilearn.common.ApiResponse;
import com.verilearn.knowledge.dto.KnowledgeNodeBatchUpsertRequest;
import com.verilearn.knowledge.dto.KnowledgeNodeConfirmResponse;
import com.verilearn.knowledge.dto.KnowledgeNodeResponse;
import com.verilearn.knowledge.service.KnowledgeService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/goals/{goalId}/knowledge-nodes")
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    public KnowledgeController(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    @PostMapping
    public ApiResponse<List<KnowledgeNodeResponse>> replaceKnowledgeNodes(
            @PathVariable Long goalId,
            @Valid @RequestBody KnowledgeNodeBatchUpsertRequest request
    ) {
        return ApiResponse.success(
                "knowledge nodes saved successfully",
                knowledgeService.replaceKnowledgeNodes(goalId, request)
        );
    }

    @PostMapping("/confirm")
    public ApiResponse<KnowledgeNodeConfirmResponse> confirmKnowledgeNodes(@PathVariable Long goalId) {
        return ApiResponse.success(
                "knowledge nodes confirmed successfully",
                knowledgeService.confirmKnowledgeNodes(goalId)
        );
    }

    @GetMapping
    public ApiResponse<List<KnowledgeNodeResponse>> getKnowledgeNodes(@PathVariable Long goalId) {
        return ApiResponse.success(knowledgeService.listKnowledgeNodes(goalId));
    }
}
