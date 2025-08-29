package org.linghu.mybackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@SpringBootApplication
@EnableMethodSecurity
public class LinHuZhiYanApplication {

    public static void main(String[] args) {
        SpringApplication.run(LinHuZhiYanApplication.class, args);
    }

}
