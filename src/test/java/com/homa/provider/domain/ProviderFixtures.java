package com.homa.provider.domain;

import static com.homa.fixtures.HomaFixtures.CHECK_IN_PERIOD;
import static com.homa.fixtures.HomaFixtures.FARID_MEMBER_ID;
import static com.homa.fixtures.HomaFixtures.JOB_END;
import static com.homa.fixtures.HomaFixtures.KEMAS_HOME_PROVIDER_ID;
import static com.homa.fixtures.HomaFixtures.NURUL_MEMBER_ID;
import static com.homa.fixtures.HomaFixtures.RINA_MEMBER_ID;
import static com.homa.fixtures.HomaFixtures.SITI_MEMBER_ID;
import static com.homa.fixtures.HomaFixtures.at;

import com.homa.kernel.TimeInterval;
import com.homa.provider.domain.Eligibility.ForeignWorker;
import com.homa.provider.domain.Eligibility.Local;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * The providers and people of the example setup (§0), built from {@link com.homa.fixtures.HomaFixtures}. Kemas Home
 * Sdn Bhd is a Business provider run by its owner Azman, with helpers Siti and Nurul (local), helper Rina (foreign) and
 * supervisor Farid (local). Hana Cleaning is run by its owner Hana. Everyone here has passed identity verification;
 * the facts are what Verification decided about them. Kemas Home adds its team on Tuesday 1 September 2026 at 14:00,
 * Kuala Lumpur time, when Rina's permit has 200 days left (HLP-3): it expires on 20 March 2027 (ROS-4).
 */
final class ProviderFixtures {

    private ProviderFixtures() {
    }

    static final ProviderId KEMAS_HOME = new ProviderId(KEMAS_HOME_PROVIDER_ID);
    static final ProviderId HANA_CLEANING = new ProviderId("provider-hana-cleaning");

    static final PersonId AZMAN = new PersonId("person-azman");
    static final PersonId HANA = new PersonId("person-hana");
    static final PersonId SITI_PERSON = new PersonId("person-siti");
    static final PersonId NURUL_PERSON = new PersonId("person-nurul");
    static final PersonId RINA_PERSON = new PersonId("person-rina");
    static final PersonId FARID_PERSON = new PersonId("person-farid");

    static final TeamMemberId SITI = new TeamMemberId(SITI_MEMBER_ID);
    static final TeamMemberId NURUL = new TeamMemberId(NURUL_MEMBER_ID);
    static final TeamMemberId RINA = new TeamMemberId(RINA_MEMBER_ID);
    static final TeamMemberId FARID = new TeamMemberId(FARID_MEMBER_ID);

    static final Instant TEAM_ADDED_AT = at(2026, 9, 1, 14, 0);

    /** Everyone passed their verification on Tuesday 25 August 2026; it falls due for re-verification in six months. */
    static final Instant VERIFIED_UNTIL = at(2027, 2, 25, 9, 0);
    static final LocalDate TEAM_ADDED_ON = LocalDate.of(2026, 9, 1);

    /** Rina's permit, the date on her i-Kad, has 200 days left when she is added (HLP-3). */
    static final LocalDate RINA_PERMIT_EXPIRY = LocalDate.of(2027, 3, 20);

    /** Azman, verified with his MyKad: local and aged 18 or above. */
    static VerifiedPersonFacts azman() {
        return VerifiedPersonFacts.local(AZMAN, true, VERIFIED_UNTIL);
    }

    /** Hana, verified with her MyKad: local and aged 18 or above. */
    static VerifiedPersonFacts hana() {
        return VerifiedPersonFacts.local(HANA, true, VERIFIED_UNTIL);
    }

    static VerifiedPersonFacts sitiVerified() {
        return VerifiedPersonFacts.local(SITI_PERSON, true, VERIFIED_UNTIL);
    }

    static VerifiedPersonFacts nurulVerified() {
        return VerifiedPersonFacts.local(NURUL_PERSON, true, VERIFIED_UNTIL);
    }

    /** Rina, verified with her i-Kad: foreign, aged 18 or above, permit until 20 March 2027. */
    static VerifiedPersonFacts rinaVerified() {
        return VerifiedPersonFacts.foreign(RINA_PERSON, true, RINA_PERMIT_EXPIRY, VERIFIED_UNTIL);
    }

    static VerifiedPersonFacts faridVerified() {
        return VerifiedPersonFacts.local(FARID_PERSON, true, VERIFIED_UNTIL);
    }

    /** Azman has opened Kemas Home; it has no team and no payout account yet. */
    static Provider openedKemasHome() {
        return Provider.open(KEMAS_HOME, azman(), Affiliation.none());
    }

    /** Hana has opened Hana Cleaning; it has no team and no payout account yet. */
    static Provider openedHanaCleaning() {
        return Provider.open(HANA_CLEANING, hana(), Affiliation.none());
    }

    /** Kemas Home's payout account, in the company's name, is verified; it has not gone live. */
    static Provider kemasHomeWithVerifiedPayoutAccount() {
        Provider kemasHome = openedKemasHome();
        kemasHome.recordPayoutAccountVerified(PayoutAccountHolder.BUSINESS);
        return kemasHome;
    }

    /** Kemas Home is live with Siti, Nurul and Rina, no supervisor, and its company payout account verified. */
    static Provider liveKemasHome() {
        Provider kemasHome = kemasHomeWithVerifiedPayoutAccount();
        kemasHome.goLive(helpers(kemasHome).activeHelperCount());
        return kemasHome;
    }

    static TeamMember siti(Provider provider) {
        return provider.addTeamMember(SITI, TeamRole.HELPER, sitiVerified(), false, Affiliation.none(), TEAM_ADDED_AT);
    }

    static TeamMember nurul(Provider provider) {
        return provider.addTeamMember(NURUL, TeamRole.HELPER, nurulVerified(), false, Affiliation.none(), TEAM_ADDED_AT);
    }

    /** Rina, with Kemas Home's confirmation that she works lawfully for it (HLP-4). */
    static TeamMember rina(Provider provider) {
        return provider.addTeamMember(RINA, TeamRole.HELPER, rinaVerified(), true, Affiliation.none(), TEAM_ADDED_AT);
    }

    static TeamMember farid(Provider provider) {
        return provider.addTeamMember(FARID, TeamRole.SUPERVISOR, faridVerified(), false, Affiliation.none(), TEAM_ADDED_AT);
    }

    /** Siti, Nurul and Rina, all active. */
    static Team helpers(Provider provider) {
        return new Team(List.of(siti(provider), nurul(provider), rina(provider)));
    }

    /** Rina's eligibility: her permit, with Kemas Home's confirmation (HLP-3, HLP-4). */
    static ForeignWorker rinaPermit() {
        return new ForeignWorker(RINA_PERMIT_EXPIRY, true);
    }

    /** A stored local member of Kemas Home with a verified photo, as the persistence adapter would rebuild it. */
    static TeamMember storedMember(TeamMemberId id, TeamRole role, boolean deactivated, boolean suspended) {
        return storedMember(id, role, deactivated, suspended, false);
    }

    /** The same, removed or not (HLP-8). */
    static TeamMember storedMember(
            TeamMemberId id, TeamRole role, boolean deactivated, boolean suspended, boolean removed) {
        return TeamMember.restore(id, KEMAS_HOME, SITI_PERSON, role, new Local(), TEAM_ADDED_AT, true, deactivated,
                suspended, removed, VERIFIED_UNTIL);
    }

    /** The standard reservation's window, Saturday 5 December 2026: the Check-in Period from 9:45 to the job end at 14:00. */
    static RosteredWindows standardJobWindow() {
        return RosteredWindows.of(List.of(new TimeInterval(CHECK_IN_PERIOD.start(), JOB_END)));
    }
}
