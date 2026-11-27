/**
 * Verification's domain: who a person is, verified once and recognised by personId across every role (plan,
 * "Owner, identity and verification"). It imports only {@code com.homa.kernel} and {@code java.*}.
 *
 * <p>Built (slice 1, minimal): {@link com.homa.verification.domain.CountryPolicy} with Malaysia (§14);
 * {@link com.homa.verification.domain.VerifiedPerson} (IDN-1, IDN-2, IDN-3, CUS-10's identity part, PRV-1,
 * HLP-1 to HLP-3); {@link com.homa.verification.domain.IdentityNumberStatus} (one number, one person);
 * {@link com.homa.verification.domain.AgeRequirement}; {@link com.homa.verification.domain.VerificationValues};
 * {@link com.homa.verification.domain.VerificationCycle#nextDueAt}.
 *
 * <p>Hooks, left room for and not built:
 * <ul>
 *   <li><b>VerificationCycle</b> (MON-1, MON-2, MON-5, slice 6): reminders from 60 days before, the 14-day deadline
 *       once flagged, and {@code deadlineFor(..., outages)}.</li>
 *   <li><b>Flags</b> (MON-4, slice 6): a monitoring flag suspends the person at once and only Support clears it; it
 *       becomes state on {@code VerifiedPerson}, published as {@code PersonFlaggedIE} and {@code PersonFlagClearedIE}.</li>
 *   <li><b>BusinessVerification</b> (PRV-7, MON-5, MON-6, slice 6): an aggregate per provider holding the
 *       registration with the policy's {@code BusinessRegistry}, the owner's link checked against the owner's
 *       {@code VerifiedPerson}, and its 6-month cycle. A failed result can be resubmitted with corrected details and
 *       gets a new result (docs/decisions/0003), so it holds a current submission and its result, not a final verdict.</li>
 *   <li><b>Profile photos</b> (IDN-4, slice 9): an owner's or member's verified photo on {@code VerifiedPerson}; a new
 *       photo replaces it only once verified. A customer's photo stays in Customer.</li>
 *   <li><b>Outage log</b> (EXT-5, slice 10): {@code VerificationOutageLog} of outage intervals, which move deadlines.</li>
 *   <li><b>IdentityNumberViewLog</b> (IDN-3, DAT-14, slice 9): every Support view of a number, kept until 12 months
 *       after the number is deleted.</li>
 *   <li><b>Identity-number block</b> (DAT-7, slice 6): a {@code BLOCKED} state on {@code IdentityNumberStatus}.</li>
 *   <li><b>PrivacyConsent</b> (DAT-11, slice 9): notice version, one of the policy's {@code NoticeLanguage}s, agreedAt.</li>
 *   <li><b>Retention</b> (DAT-9, DAT-14, slice 9): {@code VerificationRecordRetention} and
 *       {@code IdentityNumberViewRetention}.</li>
 * </ul>
 */
package com.homa.verification.domain;
