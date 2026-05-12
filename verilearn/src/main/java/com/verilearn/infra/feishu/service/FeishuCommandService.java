package com.verilearn.infra.feishu.service;

import com.verilearn.infra.feishu.dto.FeishuCommandResponse;
import com.verilearn.infra.feishu.dto.FeishuEventRequest;

public interface FeishuCommandService {

    void verifyTokenIfNecessary(FeishuEventRequest request);

    FeishuCommandResponse handleCommand(FeishuEventRequest request);
}
