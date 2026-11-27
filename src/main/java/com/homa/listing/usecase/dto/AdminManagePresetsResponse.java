package com.homa.listing.usecase.dto;

import java.util.List;

/** Homa's whole catalogue after the change, each list sorted by code. */
public record AdminManagePresetsResponse(List<String> jobTypes, List<String> tools, List<String> cities) {

    public AdminManagePresetsResponse {
        jobTypes = List.copyOf(jobTypes);
        tools = List.copyOf(tools);
        cities = List.copyOf(cities);
    }
}
