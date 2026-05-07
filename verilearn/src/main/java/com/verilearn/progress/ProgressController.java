package com.verilearn.progress;

import com.verilearn.common.ApiResponse;
import com.verilearn.progress.dto.ProgressResponse;
import com.verilearn.progress.service.ProgressService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/progress")
public class ProgressController {

    private final ProgressService progressService;

    public ProgressController(ProgressService progressService) {
        this.progressService = progressService;
    }

    @GetMapping("/{userId}")
    public ApiResponse<ProgressResponse> getProgress(@PathVariable Long userId) {
        return ApiResponse.success(progressService.getProgress(userId));
    }
}
