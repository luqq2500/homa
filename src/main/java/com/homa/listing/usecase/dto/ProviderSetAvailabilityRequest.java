package com.homa.listing.usecase.dto;

import java.util.List;

/**
 * The provider's working days and hours, replacing the previous ones (OFR-8).
 *
 * @param weeklyHours one entry per working day, each "DAY HH:MM-HH:MM" in Kuala Lumpur time, such as
 *                    "SATURDAY 08:00-18:00"; a day not listed isn't a working day
 */
public record ProviderSetAvailabilityRequest(String actingPersonId, String providerId, List<String> weeklyHours) {
}
