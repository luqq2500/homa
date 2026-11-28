package com.homa.provider.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/**
 * What Verification decided about a person who passed identity verification (IDN-1), as Provider receives it from
 * {@code PersonVerifiedIE}. Provider never decides these facts itself: Verification checks the identity document the
 * country policy sets for the person's status (§14), whether they are aged 18 or above, and, for a foreign person,
 * the work permit's expiry. Provider judges them against its own rules: who may own a provider (PRV-1) and who may
 * join a team (HLP-1 to HLP-3).
 *
 * @param personId       the person, as Verification knows them
 * @param local          true when verified as local, false when verified as foreign (§1, §14)
 * @param ageCheckPassed true when Verification found the person aged 18 or above (PRV-1, HLP-1)
 * @param permitExpiry   a foreign person's work permit expiry, the date on their i-Kad (§14); null for a local person
 * @param verifiedUntil  when the verification falls due for re-verification (MON-1), as {@code nextDueAt} on
 *                       {@code PersonVerifiedIE}; the verification is valid until then (HLP-7)
 */
public record VerifiedPersonFacts(
        PersonId personId, boolean local, boolean ageCheckPassed, LocalDate permitExpiry, Instant verifiedUntil) {

    public VerifiedPersonFacts {
        Objects.requireNonNull(personId, "personId");
        Objects.requireNonNull(verifiedUntil, "verifiedUntil");
        if (local == (permitExpiry != null)) {
            throw new IllegalArgumentException(
                    "A local person has no work permit, and a foreign person always has its expiry (§14)");
        }
    }

    /** A person verified as local, with the age check's result. */
    public static VerifiedPersonFacts local(PersonId personId, boolean ageCheckPassed, Instant verifiedUntil) {
        return new VerifiedPersonFacts(personId, true, ageCheckPassed, null, verifiedUntil);
    }

    /** A person verified as foreign, with the age check's result and their work permit's expiry. */
    public static VerifiedPersonFacts foreign(
            PersonId personId, boolean ageCheckPassed, LocalDate permitExpiry, Instant verifiedUntil) {
        return new VerifiedPersonFacts(personId, false, ageCheckPassed, permitExpiry, verifiedUntil);
    }
}
