package com.aziz.demosec;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableScheduling   // ← add this
public class DemosecApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemosecApplication.class, args);
    }
}