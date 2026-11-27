package com.homa.provider.usecase;

import static com.homa.fixtures.HomaFixtures.KEMAS_HOME_PROVIDER_ID;
import static com.homa.fixtures.HomaFixtures.at;

import com.homa.ie.events.PayoutAccountVerifiedIE;
import com.homa.ie.events.PersonVerifiedIE;
import com.homa.ie.events.ProfilePhotoChangedIE;
import com.homa.provider.domain.Owner;
import com.homa.provider.domain.PayoutAccountHolder;
import com.homa.provider.domain.PersonId;
import com.homa.provider.domain.Provider;
import com.homa.provider.domain.ProviderId;
import com.homa.provider.domain.ProviderStatus;
import com.homa.provider.domain.VerifiedPersonFacts;
import com.homa.provider.usecase.port.out.VerifiedPerson;
import java.time.Instant;
import java.time.LocalDate;

/**
 * The people of the example setup (§0) as Provider's use cases meet them: Azman, who owns Kemas Home; helpers Siti
 * and Nurul (local) and Rina (foreign); supervisor Farid (local); and Hana, who runs Hana Cleaning. Kemas Home builds
 * its team on Tuesday 1 September 2026 at 14:00, Kuala Lumpur time, when Rina's permit, expiring on 20 March 2027, has
 * 200 days left (HLP-3), so she can be rostered on jobs ending by 20 December 2026 (ROS-4).
 */
public final class ProviderUsecaseFixtures {

    private ProviderUsecaseFixtures() {
    }

    public static final ProviderId KEMAS_HOME = new ProviderId(KEMAS_HOME_PROVIDER_ID);

    public static final String AZMAN = "person-azman";
    public static final String HANA = "person-hana";
    public static final String SITI = "person-siti";
    public static final String NURUL = "person-nurul";
    public static final String RINA = "person-rina";
    public static final String FARID = "person-farid";

    public static final Instant VERIFIED_AT = at(2026, 8, 25, 9, 0);
    public static final Instant NEXT_DUE_AT = at(2027, 2, 25, 9, 0);
    public static final Instant TEAM_ADDED_AT = at(2026, 9, 1, 14, 0);
    public static final LocalDate RINA_PERMIT_EXPIRY = LocalDate.of(2027, 3, 20);
    public static final LocalDate RINA_ROSTERABLE_UNTIL = LocalDate.of(2026, 12, 20);

    /** Verification's word that a person passed identity verification as an owner or member. */
    public static PersonVerifiedIE personVerified(String eventId, String personId, String role, String name,
            String status, boolean ageCheckPassed, LocalDate permitExpiry) {
        return new PersonVerifiedIE(eventId, personId, role, null, name, status, ageCheckPassed,
                permitExpiry == null ? null : permitExpiry.toString(), NEXT_DUE_AT, VERIFIED_AT);
    }

    /** Azman verifies with his MyKad to open Kemas Home: local, aged 18 or above. */
    public static PersonVerifiedIE azmanVerified(String eventId) {
        return personVerified(eventId, AZMAN, "OWNER", "Azman", "LOCAL", true, null);
    }

    /** A local member verifies with their MyKad. */
    public static PersonVerifiedIE localMemberVerified(String eventId, String personId, String name) {
        return personVerified(eventId, personId, "MEMBER", name, "LOCAL", true, null);
    }

    /** Rina verifies with her i-Kad: foreign, aged 18 or above, permit until 20 March 2027. */
    public static PersonVerifiedIE rinaVerified(String eventId) {
        return personVerified(eventId, RINA, "MEMBER", "Rina", "FOREIGN", true, RINA_PERMIT_EXPIRY);
    }

    public static PayoutAccountVerifiedIE payoutAccountVerified(String eventId, String holder, boolean verified) {
        return new PayoutAccountVerifiedIE(eventId, KEMAS_HOME.value(), "payout-account-kemas-home", holder, verified,
                at(2026, 9, 2, 10, 0));
    }

    public static ProfilePhotoChangedIE photoChanged(String eventId, String personId, boolean verified) {
        return new ProfilePhotoChangedIE(eventId, personId, verified, "photo-" + personId, at(2026, 8, 26, 9, 0));
    }

    public static VerifiedPerson verifiedLocal(String personId, String name) {
        return new VerifiedPerson(VerifiedPersonFacts.local(new PersonId(personId), true), name);
    }

    /** Kemas Home as stored, run by Azman. {@code payoutAccount} is null while it has no verified payout account. */
    public static Provider kemasHome(ProviderStatus status, PayoutAccountHolder payoutAccount) {
        return Provider.restore(KEMAS_HOME, new Owner(new PersonId(AZMAN)), status, payoutAccount);
    }
}
