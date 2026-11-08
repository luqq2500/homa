package com.homa.offering.domain.exception;

/** A provider can only cover cities Homa serves. */
public class CityNotInServiceAreaException extends OfferingRuleViolationException {

    public CityNotInServiceAreaException(Object city) {
        super(city + " is not in Homa's service area");
    }
}
