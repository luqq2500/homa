package com.homa.offering.domain.exception;

/** A provider offers at least one job. */
public class NoJobsOfferedException extends OfferingRuleViolationException {

    public NoJobsOfferedException() {
        super("A provider must offer at least one job");
    }
}
