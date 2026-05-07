package com.verilearn.progress.service;

import com.verilearn.progress.dto.ProgressResponse;

public interface ProgressService {

    ProgressResponse getProgress(Long userId);
}
