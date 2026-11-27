package com.homa.provider.usecase.dto;

import java.time.LocalDate;

/**
 * The member just added, active.
 *
 * @param role            HELPER or SUPERVISOR
 * @param eligibility     LOCAL or FOREIGN_WORKER
 * @param photoVerified   true when the member's face photo is already verified (IDN-4)
 * @param rosterableUntil the last Kuala Lumpur date a rostered job may end on (ROS-4); null for a local member
 */
public record AddTeamMemberResponse(
        String memberId,
        String providerId,
        String personId,
        String role,
        String eligibility,
        boolean photoVerified,
        LocalDate rosterableUntil) {
}
