package edu.csuf.sis.behavior;

import io.cucumber.java.ParameterType;

import java.time.LocalDate;
import java.time.LocalTime;

public class ParameterTypes {

    @ParameterType("\\d{4}-\\d{2}-\\d{2}")
    public LocalDate date(String value) {
        return LocalDate.parse(value);
    }

    @ParameterType("\\d{2}:\\d{2}")
    public LocalTime time(String value) {
        return LocalTime.parse(value);
    }
}
