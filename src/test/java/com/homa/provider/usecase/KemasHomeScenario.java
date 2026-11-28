package com.homa.provider.usecase;

import static com.homa.provider.usecase.ProviderUsecaseFixtures.FARID;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.KEMAS_HOME;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.NEXT_DUE_AT;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.NURUL;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.RINA;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.RINA_PERMIT_EXPIRY;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.SITI;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.TEAM_ADDED_AT;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.kemasHome;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.storedMember;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.verifiedLocal;

import com.homa.provider.domain.Eligibility.ForeignWorker;
import com.homa.provider.domain.PayoutAccountHolder;
import com.homa.provider.domain.PersonId;
import com.homa.provider.domain.ProviderStatus;
import com.homa.provider.domain.TeamMember;
import com.homa.provider.domain.TeamMemberId;
import com.homa.provider.domain.TeamRole;
import com.homa.provider.domain.VerifiedPersonFacts;
import com.homa.provider.usecase.port.out.VerifiedPerson;

/**
 * Kemas Home as the slice 4 use cases meet it, on Tuesday 1 September 2026 at 14:00: live, with its company payout
 * account verified, run by Azman, with helpers Siti and Nurul and supervisor Farid, all active and all verified until
 * 25 February 2027. Rina, its foreign helper, is added by the tests that need her.
 */
final class KemasHomeScenario {

    static final String SITI_MEMBER = "member-siti";
    static final String NURUL_MEMBER = "member-nurul";
    static final String FARID_MEMBER = "member-farid";
    static final String RINA_MEMBER = "member-rina";

    private KemasHomeScenario() {
    }

    /** Kemas Home is live with Siti, Nurul and Farid, and their verification is known. */
    static void liveWithSitiNurulAndFarid(ProviderWorld world) {
        world.providers.save(kemasHome(ProviderStatus.LIVE, PayoutAccountHolder.BUSINESS));
        world.verifiedPeople.save(verifiedLocal(SITI, "Siti"));
        world.verifiedPeople.save(verifiedLocal(NURUL, "Nurul"));
        world.verifiedPeople.save(verifiedLocal(FARID, "Farid"));
        world.members.save(storedMember(SITI_MEMBER, SITI, TeamRole.HELPER, false, false, false));
        world.members.save(storedMember(NURUL_MEMBER, NURUL, TeamRole.HELPER, false, false, false));
        world.members.save(storedMember(FARID_MEMBER, FARID, TeamRole.SUPERVISOR, false, false, false));
    }

    /** Rina, foreign, with Kemas Home's confirmation, is on the team as a helper. */
    static void withRina(ProviderWorld world) {
        world.verifiedPeople.save(new VerifiedPerson(
                VerifiedPersonFacts.foreign(new PersonId(RINA), true, RINA_PERMIT_EXPIRY, NEXT_DUE_AT), "Rina"));
        world.members.save(TeamMember.restore(new TeamMemberId(RINA_MEMBER), KEMAS_HOME, new PersonId(RINA),
                TeamRole.HELPER, new ForeignWorker(RINA_PERMIT_EXPIRY, true), TEAM_ADDED_AT, true, false, false,
                false, NEXT_DUE_AT));
    }
}
