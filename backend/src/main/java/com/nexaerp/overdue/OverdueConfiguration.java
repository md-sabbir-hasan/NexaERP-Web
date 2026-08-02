package com.nexaerp.overdue;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class OverdueConfiguration {

    @Bean
    Clock overdueClock(OverdueProperties properties) {
        return Clock.system(ZoneId.of(properties.getTimeZone()));
    }
}
