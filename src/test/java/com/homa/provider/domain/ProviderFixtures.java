package com.homa.provider.domain;

import static com.homa.fixtures.HomaFixtures.FARID_MEMBER_ID;
import static com.homa.fixtures.HomaFixtures.KEMAS_HOME_NAME;
import static com.homa.fixtures.HomaFixtures.KEMAS_HOME_PROVIDER_ID;
import static com.homa.fixtures.HomaFixtures.KEMAS_HOME_REGISTRATION_NO;
import static com.homa.fixtures.HomaFixtures.NURUL_MEMBER_ID;
import static com.homa.fixtures.HomaFixtures.RINA_MEMBER_ID;
import static com.homa.fixtures.HomaFixtures.SITI_MEMBER_ID;
import static com.homa.fixtures.HomaFixtures.at;

import com.homa.provider.domain.Eligibility.Citizen;
import com.homa.provider.domain.Eligibility.ForeignWorker;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Kemas Home Sdn Bhd and its team from the example setup (§0), built from {@link com.homa.fixtures.HomaFixtures}:
 * helpers Siti and Nurul (citizens), helper Rina (foreign) and supervisor Farid (citizen). Kemas Home's business
 * is verified on Monday 2 November 2026 at 10:00 and its team the same afternoon at 14:00, Kuala Lumpur time.
 */
final class ProviderFixtures {

    private ProviderFixtures() {
    }

    static final ProviderId KEMAS_HOME = new ProviderId(KEMAS_HOME_PROVIDER_ID);
    static final TeamMemberId SITI = new TeamMemberId(SITI_MEMBER_ID);
    static final TeamMemberId NURUL = new TeamMemberId(NURUL_MEMBER_ID);
    static final TeamMemberId RINA = new TeamMemberId(RINA_MEMBER_ID);
    static final TeamMemberId FARID = new TeamMemberId(FARID_MEMBER_ID);

    static final Instant BUSINESS_VERIFIED_AT = at(2026, 11, 2, 10, 0);
    static final Instant TEAM_VERIFIED_AT = at(2026, 11, 2, 14, 0);
    static final LocalDate TEAM_VERIFIED_ON = LocalDate.of(2026, 11, 2);

    static final LocalDate SITI_BORN = LocalDate.of(1994, 6, 12);
    static final LocalDate NURUL_BORN = LocalDate.of(1998, 1, 25);
    static final LocalDate RINA_BORN = LocalDate.of(1996, 9, 3);
    static final LocalDate FARID_BORN = LocalDate.of(1985, 4, 18);

    /** Rina's permit expires 200 days after she is verified (HLP-3 case). */
    static final LocalDate RINA_PERMIT_EXPIRY = TEAM_VERIFIED_ON.plusDays(200);

    static BusinessRegistration kemasHomeRegistration() {
        return new BusinessRegistration(KEMAS_HOME_REGISTRATION_NO, KEMAS_HOME_NAME);
    }

    /** Kemas Home has submitted its company registration and awaits business verification. */
    static Provider registeredKemasHome() {
        return Provider.register(KEMAS_HOME, kemasHomeRegistration());
    }

    /** Kemas Home's business is verified; it has no team yet. */
    static Provider verifiedKemasHome() {
        Provider kemasHome = registeredKemasHome();
        kemasHome.recordBusinessVerification(VerificationOutcome.PASSED, BUSINESS_VERIFIED_AT);
        return kemasHome;
    }

    /** Kemas Home is live with Siti, Nurul and Rina, and no supervisor. */
    static Provider liveKemasHome() {
        Provider kemasHome = verifiedKemasHome();
        kemasHome.goLive(helpers(kemasHome).activeHelperCount());
        return kemasHome;
    }

    static MemberVerification passedVerification(LocalDate dateOfBirth) {
        return new MemberVerification(VerificationOutcome.PASSED, dateOfBirth, TEAM_VERIFIED_AT);
    }

    static MemberVerification failedVerification(LocalDate dateOfBirth) {
        return new MemberVerification(VerificationOutcome.FAILED, dateOfBirth, TEAM_VERIFIED_AT);
    }

    static Eligibility citizen() {
        return new Citizen();
    }

    /** Rina's permit, with Kemas Home's confirmation that it lawfully employs her (HLP-4). */
    static ForeignWorker rinaPermit() {
        return new ForeignWorker(RINA_PERMIT_EXPIRY, true);
    }

    static TeamMember siti(Provider provider) {
        return provider.addTeamMember(SITI, TeamRole.HELPER, citizen(), passedVerification(SITI_BORN));
    }

    static TeamMember nurul(Provider provider) {
        return provider.addTeamMember(NURUL, TeamRole.HELPER, citizen(), passedVerification(NURUL_BORN));
    }

    static TeamMember rina(Provider provider) {
        return provider.addTeamMember(RINA, TeamRole.HELPER, rinaPermit(), passedVerification(RINA_BORN));
    }

    static TeamMember farid(Provider provider) {
        return provider.addTeamMember(FARID, TeamRole.SUPERVISOR, citizen(), passedVerification(FARID_BORN));
    }

    /** Siti, Nurul and Rina, all active. */
    static Team helpers(Provider provider) {
        return new Team(List.of(siti(provider), nurul(provider), rina(provider)));
    }

    /** A stored member of Kemas Home, as the persistence adapter would rebuild it. */
    static TeamMember storedMember(TeamMemberId id, TeamRole role, boolean deactivated, boolean suspended) {
        return TeamMember.restore(id, KEMAS_HOME, role, citizen(), TEAM_VERIFIED_AT, deactivated, suspended);
    }
}
