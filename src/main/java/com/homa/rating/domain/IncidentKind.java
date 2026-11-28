package com.homa.rating.domain;

/**
 * What reliability counts (RTG-10). Customers see a provider's cancellations, no-rosters, no-shows and members who
 * left during a job; providers see a customer's fee-paying cancellations and no-shows.
 */
public enum IncidentKind {
    /** A cancellation that counts: a provider's, or a customer's that paid a fee. */
    CANCELLATION,
    /** A reservation cancelled for want of a complete roster (NRS-1). A provider's only. */
    NO_ROSTER,
    /** A confirmed no-show (NSH-1, NSH-4). */
    NO_SHOW,
    /** A roster member who left during a job (RPT-8). A provider's only. */
    MEMBER_LEFT;

    /** Whether reliability shows this for a party on this side. */
    public boolean concerns(PartyRole role) {
        return role == PartyRole.PROVIDER || this == CANCELLATION || this == NO_SHOW;
    }
}
