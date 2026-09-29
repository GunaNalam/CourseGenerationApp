package com.learnify;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LearnifyApplication {

    public static void main(String[] args) {
        SpringApplication.run(LearnifyApplication.class, args);
    }
}
