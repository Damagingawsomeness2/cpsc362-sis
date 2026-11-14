package edu.csuf.sis;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Every date rule (add/drop deadline, session expiry) reads time from this one clock. */
@Configuration
public class ClockConfig {

    /**
     * The real clock, or one shifted to {@code sis.today} (e.g. {@code sis.today=2026-11-02})
     * so a demo can show the add/drop rules on any day of the year. Time of day keeps ticking.
     */
    @Bean
    public Clock clock(@Value("${sis.today:}") String today) {
        Clock system = Clock.systemDefaultZone();
        if (today.isBlank()) {
            return system;
        }
        long days = ChronoUnit.DAYS.between(LocalDate.now(system), LocalDate.parse(today));
        return Clock.offset(system, Duration.ofDays(days));
    }
}
