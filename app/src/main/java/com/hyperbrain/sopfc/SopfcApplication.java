package com.hyperbrain.sopfc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class SopfcApplication {
    public static void main(String[] args) {
        SpringApplication.run(SopfcApplication.class, args);
    }
}