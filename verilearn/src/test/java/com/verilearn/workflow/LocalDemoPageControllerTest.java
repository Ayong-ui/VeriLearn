package com.verilearn.workflow;

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
class LocalDemoPageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldRenderLocalDemoPage() throws Exception {
        mockMvc.perform(get("/demo/local-page"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("VeriLearn 本地演示入口")))
                .andExpect(content().string(Matchers.containsString("/api/learners/setup")))
                .andExpect(content().string(Matchers.containsString("/api/learners/${encodedOpenId}/today-task")))
                .andExpect(content().string(Matchers.containsString("/ai/provider-config-page?openId=")))
                .andExpect(content().string(Matchers.containsString("提交 Demo 反馈")));
    }
}
