package com.verilearn;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan({
        "com.verilearn.user.mapper",
        "com.verilearn.goal.mapper",
        "com.verilearn.knowledge.mapper"
})
public class VerilearnApplication {

    public static void main(String[] args) {
        SpringApplication.run(VerilearnApplication.class, args);
    }

}
