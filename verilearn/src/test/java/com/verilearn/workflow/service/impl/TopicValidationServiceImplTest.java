package com.verilearn.workflow.service.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TopicValidationServiceImplTest {

    private final TopicValidationServiceImpl topicValidationService = new TopicValidationServiceImpl();

    @Test
    void shouldRequireOptionsForBroadTopic() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> topicValidationService.validateTopicOrThrow("数学")
        );

        org.assertj.core.api.Assertions.assertThat(exception.getMessage()).contains("子方向");
    }

    @Test
    void shouldRejectGenericTopic() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> topicValidationService.validateTopicOrThrow("技术")
        );

        org.assertj.core.api.Assertions.assertThat(exception.getMessage()).contains("具体");
    }

    @Test
    void shouldAcceptSpecificTopic() {
        assertDoesNotThrow(() -> topicValidationService.validateTopicOrThrow("Spring Boot Controller"));
    }
}
