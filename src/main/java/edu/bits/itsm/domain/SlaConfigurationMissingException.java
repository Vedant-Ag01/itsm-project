package edu.bits.itsm.domain;

// Thrown when there is no SLA target saved for a priority
public class SlaConfigurationMissingException extends RuntimeException {

    public SlaConfigurationMissingException(String priority) {
        super("Missing SLA configuration for priority " + priority
                + ". An admin must set its response and resolution targets.");
    }
}
