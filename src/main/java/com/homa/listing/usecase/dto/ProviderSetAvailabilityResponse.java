package com.homa.listing.usecase.dto;

import java.util.List;

/** @param weeklyHours the working hours now in force, Monday first, each "DAY HH:MM-HH:MM" */
public record ProviderSetAvailabilityResponse(String providerId, List<String> weeklyHours) {

    public ProviderSetAvailabilityResponse {
        weeklyHours = List.copyOf(weeklyHours);
    }
}
