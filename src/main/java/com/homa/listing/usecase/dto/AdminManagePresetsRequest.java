package com.homa.listing.usecase.dto;

import java.util.List;

/**
 * A Homa admin adds job types, tools and cities providers can choose from (OFR-1).
 *
 * @param actingRole the acting person's role; only ADMIN may manage presets
 */
public record AdminManagePresetsRequest(
        String actingPersonId, String actingRole, List<String> addJobTypes, List<String> addTools, List<String> addCities) {
}
