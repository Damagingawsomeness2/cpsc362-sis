package edu.csuf.sis.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.LocalDate;
import java.time.ZoneId;

/** Replaces the system clock with one each test can set. */
@TestConfiguration(proxyBeanMethods = false)
public class TestClockConfig {

    public static final LocalDate DEFAULT_TODAY = LocalDate.of(2026, 11, 2);

    @Bean
    @Primary
    public MutableClock testClock() {
        return new MutableClock(DEFAULT_TODAY, ZoneId.of("America/Los_Angeles"));
    }
}
