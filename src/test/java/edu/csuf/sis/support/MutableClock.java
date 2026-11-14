package edu.csuf.sis.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

/** A clock tests can set to any day (and move forward) to exercise date rules. */
public class MutableClock extends Clock {

    private final ZoneId zone;
    private volatile Instant now;

    public MutableClock(LocalDate today, ZoneId zone) {
        this.zone = zone;
        setToday(today);
    }

    /** Noon on the given day. */
    public void setToday(LocalDate today) {
        now = today.atTime(LocalTime.NOON).atZone(zone).toInstant();
    }

    public void advance(Duration duration) {
        now = now.plus(duration);
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return Clock.fixed(now, zone);
    }

    @Override
    public Instant instant() {
        return now;
    }
}
