package com.aziz.demosec.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties(prefix = "health.thresholds")
@Getter @Setter
public class HealthThresholds {
    private int overeatingSurplus = 300;      // kcal above expected
    private int undereatingSurplus = 300;     // kcal below expected
    private double noProgressWeightDelta = 0.1; // kg change minimum
    private int noProgressDaysWindow = 7;     // days to check weight progress
}
