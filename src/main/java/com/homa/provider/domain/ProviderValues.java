package com.homa.provider.domain;

/**
 * The Homa-set values Provider owns (§13 of the business rules). Both are account values: they apply at once to
 * everyone, so each has one current value (P3). Other contexts never copy these numbers; they receive the computed
 * result, such as {@code rosterableUntil} on team-member events.
 *
 * <p>The minimum age and the re-verification cycles belong to Verification (plan, "Homa-set values"); Provider only
 * reads the age check's result from {@link VerifiedPersonFacts}. The deprecated constants below stay only until
 * {@code HomaSetValuesTest} reads them from Verification. No Provider rule uses them.
 */
public final class ProviderValues {

    /** Days a work permit must still run when a foreign member is added, and after a rostered job ends (HLP-3, ROS-4). */
    public static final int WORK_PERMIT_BUFFER_DAYS = 90;

    /** Active helpers a provider needs to go live and stay bookable (PRV-2). */
    public static final int MINIMUM_ACTIVE_HELPERS = 1;

    private ProviderValues() {
    }
}
