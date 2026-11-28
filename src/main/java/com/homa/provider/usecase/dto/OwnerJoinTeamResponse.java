package com.homa.provider.usecase.dto;

import java.time.LocalDate;

/**
 * The owner as a member of their own team, active.
 *
 * @param role            HELPER or SUPERVISOR
 * @param eligibility     LOCAL: an owner is always local (PRV-1)
 * @param photoVerified   true when the owner's face photo is already verified (IDN-4)
 * @param rosterableUntil the last Kuala Lumpur date a rostered job may end on (ROS-4); null for a local member
 */
public record OwnerJoinTeamResponse(
        String memberId,
        String providerId,
        String personId,
        String role,
        String eligibility,
        boolean photoVerified,
        LocalDate rosterableUntil) {
}
