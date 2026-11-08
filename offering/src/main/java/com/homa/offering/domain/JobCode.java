package com.homa.offering.domain;

/** A job from Homa's job list, by its code, such as {@code KITCHEN_GREASE}. */
public record JobCode(String value) {

    public JobCode {
        value = OfferingText.require(value, "A job code");
    }

    public static JobCode of(String value) {
        return new JobCode(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
