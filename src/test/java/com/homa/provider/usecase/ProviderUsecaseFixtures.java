package com.homa.provider.usecase;

import static com.homa.fixtures.HomaFixtures.KEMAS_HOME_PROVIDER_ID;
import static com.homa.fixtures.HomaFixtures.at;

import com.homa.ie.events.PayoutAccountVerifiedIE;
import com.homa.ie.events.PersonVerifiedIE;
import com.homa.ie.events.ProfilePhotoChangedIE;
import com.homa.ie.events.ReservationCancelledIE;
import com.homa.ie.events.ReservationCompletedIE;
import com.homa.ie.events.ReservationReservedIE;
import com.homa.ie.events.RosterChangedIE;
import com.homa.kernel.Money;
import com.homa.provider.domain.Eligibility;
import com.homa.provider.domain.Owner;
import com.homa.provider.domain.PayoutAccountHolder;
import com.homa.provider.domain.PersonId;
import com.homa.provider.domain.Provider;
import com.homa.provider.domain.ProviderId;
import com.homa.provider.domain.ProviderStatus;
import com.homa.provider.domain.TeamMember;
import com.homa.provider.domain.TeamMemberId;
import com.homa.provider.domain.TeamRole;
import com.homa.provider.domain.VerifiedPersonFacts;
import com.homa.provider.usecase.port.out.VerifiedPerson;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

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
        return new VerifiedPerson(VerifiedPersonFacts.local(new PersonId(personId), true, NEXT_DUE_AT), name);
    }

    /** Kemas Home as stored, run by Azman. {@code payoutAccount} is null while it has no verified payout account. */
    public static Provider kemasHome(ProviderStatus status, PayoutAccountHolder payoutAccount) {
        return Provider.restore(KEMAS_HOME, new Owner(new PersonId(AZMAN)), status, payoutAccount);
    }

    public static final String SATURDAY_RESERVATION = "reservation-saturday";
    public static final Instant SATURDAY_CHECK_IN_START = at(2026, 12, 5, 9, 45);
    public static final Instant SATURDAY_JOB_START = at(2026, 12, 5, 10, 0);
    public static final Instant SATURDAY_JOB_END = at(2026, 12, 5, 14, 0);

    /** A member of Kemas Home as stored: a local helper or supervisor, with a verified photo, added on 1 September. */
    public static TeamMember storedMember(String memberId, String personId, TeamRole role, boolean deactivated,
            boolean suspended, boolean removed) {
        return TeamMember.restore(new TeamMemberId(memberId), KEMAS_HOME, new PersonId(personId), role,
                new Eligibility.Local(), TEAM_ADDED_AT, true, deactivated, suspended, removed, NEXT_DUE_AT);
    }

    /** The standard reservation, Saturday 5 December 10:00 to 14:00 with its Check-in Period from 9:45, is Reserved. */
    public static ReservationReservedIE saturdayReserved(String eventId) {
        return new ReservationReservedIE(eventId, SATURDAY_RESERVATION, "customer-aisyah", KEMAS_HOME.value(),
                SATURDAY_CHECK_IN_START, SATURDAY_JOB_START, SATURDAY_JOB_END, Money.myr(200),
                at(2026, 12, 6, 14, 0), at(2026, 11, 30, 20, 0), at(2026, 12, 1, 9, 0));
    }

    /** Another job on Sunday 6 December, 10:00 to 14:00 with its Check-in Period from 9:45, is Reserved. */
    public static ReservationReservedIE sundayReserved(String eventId) {
        return new ReservationReservedIE(eventId, "reservation-sunday", "customer-aisyah", KEMAS_HOME.value(),
                at(2026, 12, 6, 9, 45), at(2026, 12, 6, 10, 0), at(2026, 12, 6, 14, 0), Money.myr(200),
                at(2026, 12, 7, 14, 0), at(2026, 12, 1, 20, 0), at(2026, 12, 1, 21, 0));
    }

    /** Kemas Home sets or changes the roster of a reservation. */
    public static RosterChangedIE rosterChanged(String eventId, String reservationId, String... memberIds) {
        return new RosterChangedIE(eventId, reservationId, List.of(memberIds), at(2026, 12, 4, 17, 0));
    }

    /** The job is marked Completed. */
    public static ReservationCompletedIE completed(String eventId, String reservationId) {
        return new ReservationCompletedIE(eventId, reservationId, "customer-aisyah", KEMAS_HOME.value(),
                Money.myr(200), at(2026, 12, 6, 14, 0), SATURDAY_JOB_END, at(2026, 11, 30, 20, 0),
                List.of("photo-after"), SATURDAY_JOB_END);
    }

    /** The reservation is Cancelled, by the provider, before its Check-in Period. */
    public static ReservationCancelledIE cancelled(String eventId, String reservationId) {
        return new ReservationCancelledIE(eventId, reservationId, "customer-aisyah", KEMAS_HOME.value(),
                "PROVIDER_CANCELLED", at(2026, 12, 3, 10, 0), at(2026, 11, 30, 20, 0), SATURDAY_CHECK_IN_START,
                at(2026, 12, 6, 14, 0), "settlement-1", Money.myr(0), Money.myr(200), Money.myr(0), Money.myr(45),
                Money.myr(0), at(2026, 12, 3, 10, 0));
    }
}
