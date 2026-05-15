package com.verilearn.infra.feishu;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FeishuLocalSetupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnLocalSetupChecklist() throws Exception {
        mockMvc.perform(get("/api/feishu/local-setup"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.callbackPath").value("/api/feishu/events"))
                .andExpect(jsonPath("$.data.callbackUrlTemplate").value("{PUBLIC_BASE_URL}/api/feishu/events"))
                .andExpect(jsonPath("$.data.aiConfigPageTemplate").value("{PUBLIC_BASE_URL}/ai/provider-config-page?openId={feishuOpenId}"))
                .andExpect(jsonPath("$.data.mode").isString())
                .andExpect(jsonPath("$.data.nextSteps").isArray())
                .andExpect(jsonPath("$.data.nextSteps.length()").value(Matchers.greaterThan(0)));
    }
}
