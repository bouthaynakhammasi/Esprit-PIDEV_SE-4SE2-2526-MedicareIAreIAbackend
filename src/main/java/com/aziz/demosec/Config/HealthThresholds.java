package com.aziz.demosec.Config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties(prefix = "health.thresholds")
@Getter @Setter
public class HealthThresholds {
    private int overeatingSurplus = 300;
    private int undereatingSurplus = 300;
    private double noProgressWeightDelta = 0.1;
    private int noProgressDaysWindow = 7;
}
