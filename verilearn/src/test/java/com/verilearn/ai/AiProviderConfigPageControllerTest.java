package com.verilearn.ai;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AiProviderConfigPageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldRenderAiProviderConfigPage() throws Exception {
        mockMvc.perform(get("/ai/provider-config-page")
                        .param("openId", "ou_test_page"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("VeriLearn AI 安全配置页")))
                .andExpect(content().string(Matchers.containsString("ou_test_page")))
                .andExpect(content().string(Matchers.containsString("/api/learners/${openId}/ai-provider-configs")))
                .andExpect(content().string(Matchers.containsString("切换到此配置")));
    }
}
