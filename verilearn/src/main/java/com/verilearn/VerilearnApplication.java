package com.verilearn;

import com.verilearn.ai.config.AiProperties;
import com.verilearn.ai.config.AiSecurityProperties;
import com.verilearn.chapter.config.StorageProperties;
import com.verilearn.infra.feishu.config.FeishuProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({AiProperties.class, AiSecurityProperties.class, FeishuProperties.class, StorageProperties.class})
@MapperScan({
        "com.verilearn.ai.mapper",
        "com.verilearn.chapter.mapper",
        "com.verilearn.user.mapper",
        "com.verilearn.goal.mapper",
        "com.verilearn.knowledge.mapper",
        "com.verilearn.task.mapper",
        "com.verilearn.validation.mapper"
})
public class VerilearnApplication {

    public static void main(String[] args) {
        SpringApplication.run(VerilearnApplication.class, args);
    }

}
