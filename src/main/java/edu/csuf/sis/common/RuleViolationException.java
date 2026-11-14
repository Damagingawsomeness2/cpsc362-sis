package edu.csuf.sis.common;

/**
 * A request broke a business rule, such as a missing prerequisite or a closed drop period.
 * The message is written for the person using the system and is shown to them as-is.
 */
public class RuleViolationException extends RuntimeException {

    public RuleViolationException(String message) {
        super(message);
    }
}
