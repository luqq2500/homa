package com.homa.provider.domain;

/**
 * The Homa-set values Provider owns (§11 of the business rules). All are account values: they apply at once to
 * everyone, so each has one current value (P3). Other contexts never copy these numbers; they receive the computed
 * result, such as {@code rosterableUntil} on team-member events.
 */
public final class ProviderValues {

    /** Minimum helper and supervisor age, in years (HLP-1). */
    public static final int MINIMUM_HELPER_AGE_YEARS = 18;

    /** Days a work permit must still run after verification, and before a rostered job ends (HLP-3, ROS-4). */
    public static final int WORK_PERMIT_BUFFER_DAYS = 90;

    /** Re-verification cycle, in months (MON-1). */
    public static final int REVERIFICATION_CYCLE_MONTHS = 6;

    /** Re-verification reminders start this many days before the due date (MON-1). */
    public static final int REVERIFICATION_REMINDER_DAYS = 60;

    /** Days a flagged provider or member has to re-verify (MON-2). */
    public static final int FLAGGED_REVERIFICATION_DAYS = 14;

    /** Active helpers a provider needs to go live and stay bookable (PRV-2, PRV-6). */
    public static final int MINIMUM_ACTIVE_HELPERS = 1;

    private ProviderValues() {
    }
}
