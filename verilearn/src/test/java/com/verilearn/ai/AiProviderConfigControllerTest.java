package com.verilearn.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.verilearn.ai.entity.LearnerAiProviderConfig;
import com.verilearn.ai.mapper.LearnerAiProviderConfigMapper;
import com.verilearn.user.entity.LearnerUser;
import com.verilearn.user.mapper.LearnerUserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AiProviderConfigControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LearnerUserMapper learnerUserMapper;

    @Autowired
    private LearnerAiProviderConfigMapper learnerAiProviderConfigMapper;

    @Test
    void shouldReturnSystemDefaultWhenUserConfigDoesNotExist() throws Exception {
        String openId = createLearner("ai-config-default-user");

        mockMvc.perform(get("/api/learners/{feishuOpenId}/ai-provider-configs/current", openId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.providerType").value("DEEPSEEK"))
                .andExpect(jsonPath("$.data.sourceType").value("SYSTEM_DEFAULT"))
                .andExpect(jsonPath("$.data.active").value(true));
    }

    @Test
    void shouldSaveEncryptedProviderConfigAndActivateSpecifiedConfig() throws Exception {
        String openId = createLearner("ai-config-switch-user");

        mockMvc.perform(post("/api/learners/{feishuOpenId}/ai-provider-configs", openId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "providerType": "DEEPSEEK",
                                  "baseUrl": "https://api.deepseek.com",
                                  "modelName": "deepseek-chat",
                                  "apiKey": "sk-test-deepseek-12345678",
                                  "activateNow": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.providerType").value("DEEPSEEK"))
                .andExpect(jsonPath("$.data.sourceType").value("USER_CONFIG"))
                .andExpect(jsonPath("$.data.active").value(true))
                .andExpect(jsonPath("$.data.apiKeyMasked").value(org.hamcrest.Matchers.containsString("***")));

        mockMvc.perform(post("/api/learners/{feishuOpenId}/ai-provider-configs", openId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "providerType": "OPENAI",
                                  "baseUrl": "https://api.openai.com/v1",
                                  "modelName": "gpt-4.1-mini",
                                  "apiKey": "sk-test-openai-abcdef123456",
                                  "activateNow": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.providerType").value("OPENAI"))
                .andExpect(jsonPath("$.data.active").value(false));

        LearnerUser learnerUser = learnerUserMapper.selectOne(
                new LambdaQueryWrapper<LearnerUser>()
                        .eq(LearnerUser::getFeishuOpenId, openId)
                        .last("LIMIT 1")
        );
        assertNotNull(learnerUser);

        List<LearnerAiProviderConfig> configs = learnerAiProviderConfigMapper.selectList(
                new LambdaQueryWrapper<LearnerAiProviderConfig>()
                        .eq(LearnerAiProviderConfig::getUserId, learnerUser.getId())
        );
        assertEquals(2, configs.size());

        LearnerAiProviderConfig deepseekConfig = configs.stream()
                .filter(config -> "DEEPSEEK".equals(config.getProviderType()))
                .findFirst()
                .orElseThrow();
        LearnerAiProviderConfig openAiConfig = configs.stream()
                .filter(config -> "OPENAI".equals(config.getProviderType()))
                .findFirst()
                .orElseThrow();

        assertNotEquals("sk-test-deepseek-12345678", deepseekConfig.getApiKeyCiphertext());
        assertEquals(true, deepseekConfig.getIsActive());
        assertEquals(false, openAiConfig.getIsActive());

        mockMvc.perform(post("/api/learners/{feishuOpenId}/ai-provider-configs/{configId}/activate", openId, openAiConfig.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.providerType").value("OPENAI"))
                .andExpect(jsonPath("$.data.active").value(true))
                .andExpect(jsonPath("$.data.apiKeyMasked").value(org.hamcrest.Matchers.containsString("***")));

        mockMvc.perform(get("/api/learners/{feishuOpenId}/ai-provider-configs/current", openId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.providerType").value("OPENAI"))
                .andExpect(jsonPath("$.data.sourceType").value("USER_CONFIG"))
                .andExpect(jsonPath("$.data.active").value(true));

        mockMvc.perform(get("/api/learners/{feishuOpenId}/ai-provider-configs", openId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[?(@.providerType=='OPENAI')].active").value(org.hamcrest.Matchers.hasItem(true)))
                .andExpect(jsonPath("$.data[?(@.providerType=='DEEPSEEK')].active").value(org.hamcrest.Matchers.hasItem(false)));
    }

    private String createLearner(String openIdPrefix) {
        String openId = openIdPrefix + "-" + System.nanoTime();
        LearnerUser learnerUser = new LearnerUser();
        learnerUser.setFeishuOpenId(openId);
        learnerUser.setCreatedAt(LocalDateTime.now());
        learnerUser.setUpdatedAt(LocalDateTime.now());
        learnerUserMapper.insert(learnerUser);
        return openId;
    }
}
