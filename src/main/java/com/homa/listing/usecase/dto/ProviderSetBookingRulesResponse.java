package com.homa.listing.usecase.dto;

/**
 * @param minimumNoticeMinutes the notice in force after the change
 * @param noticeRaised         true when switching to Request raised the notice to 2 hours; the provider is told (OFR-6)
 */
public record ProviderSetBookingRulesResponse(String providerId, long minimumNoticeMinutes, boolean noticeRaised) {
}
