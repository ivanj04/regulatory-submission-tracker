package com.ivanjiang.regtracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class RegTrackerApplication {

    public static void main(String[] args) {
        SpringApplication.run(RegTrackerApplication.class, args);
    }
}
