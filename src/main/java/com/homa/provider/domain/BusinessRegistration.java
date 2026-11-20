package com.homa.provider.domain;

import com.homa.provider.domain.exception.BusinessRegistrationRequiredException;
import java.util.Objects;

/**
 * The company registration a provider submits (PRV-1): its registration number in the country and its registered
 * business name. A business without a registration number, such as an unregistered sole trader, can't join.
 * Whether the registration belongs to a legal business entity is decided by the business verification (KYB),
 * whose result {@link Provider#recordBusinessVerification} accepts.
 */
public record BusinessRegistration(String registrationNumber, String businessName) {

    public BusinessRegistration {
        if (registrationNumber == null || registrationNumber.isBlank()) {
            throw new BusinessRegistrationRequiredException();
        }
        Objects.requireNonNull(businessName, "businessName");
        if (businessName.isBlank()) {
            throw new IllegalArgumentException("A registered business name can't be blank");
        }
    }
}
