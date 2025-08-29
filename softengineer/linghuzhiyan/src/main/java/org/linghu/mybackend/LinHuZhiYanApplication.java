package org.linghu.mybackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.linghu.mybackend.config.JudgeConfig;

@SpringBootApplication
@EnableMethodSecurity
@EnableConfigurationProperties(JudgeConfig.class)
public class LinHuZhiYanApplication {

    public static void main(String[] args) {
        SpringApplication.run(LinHuZhiYanApplication.class, args);
    }

}
