package com.homa.verification.domain;

/**
 * The Homa-set values Verification owns (§13). They are account values: they apply at once to everyone, so each
 * has one current value and no dated versions (P3). Other contexts never copy these numbers; they receive the
 * result instead, such as the age check and {@code nextDueAt} on {@code PersonVerifiedIE}.
 */
public final class VerificationValues {

    /** Minimum age of every customer, owner and member, in years (CUS-10, PRV-1, HLP-1). */
    public static final int MINIMUM_AGE_YEARS = 18;

    /** Owners, members and Business providers re-verify this many months after their last pass (MON-1, MON-5). */
    public static final int REVERIFICATION_CYCLE_MONTHS = 6;

    /** Re-verification reminders start this many days before the due date (MON-1). */
    public static final int REVERIFICATION_REMINDER_DAYS = 60;

    /** Days a flagged person has to pass a re-verification (MON-2). */
    public static final int FLAGGED_REVERIFICATION_DAYS = 14;

    private VerificationValues() {
    }
}
