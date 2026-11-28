package com.homa.provider.usecase;

import static com.homa.fixtures.HomaFixtures.at;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.AZMAN;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.HANA;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.KEMAS_HOME;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.NEXT_DUE_AT;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.SITI;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.TEAM_ADDED_AT;
import static com.homa.provider.usecase.ProviderUsecaseFixtures.verifiedLocal;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.ie.events.ProviderBookabilityChangedIE;
import com.homa.ie.events.TeamMemberActivatedIE;
import com.homa.ie.port.ProviderIE;
import com.homa.provider.domain.Owner;
import com.homa.provider.domain.PayoutAccountHolder;
import com.homa.provider.domain.PersonId;
import com.homa.provider.domain.Provider;
import com.homa.provider.domain.ProviderId;
import com.homa.provider.domain.ProviderStatus;
import com.homa.provider.domain.TeamMember;
import com.homa.provider.domain.TeamMemberId;
import com.homa.provider.domain.TeamRole;
import com.homa.provider.domain.exception.PersonAlreadyBelongsToProviderException;
import com.homa.provider.usecase.dto.OwnerJoinTeamRequest;
import com.homa.provider.usecase.dto.OwnerJoinTeamResponse;
import com.homa.provider.usecase.dto.RemoveTeamMemberRequest;
import com.homa.provider.usecase.exception.NotAllowedException;
import com.homa.provider.usecase.exception.PersonNotVerifiedException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class OwnerJoinTeamTest {

    private static final ProviderId HANA_CLEANING = new ProviderId("provider-hana-cleaning");

    private final ProviderWorld world = new ProviderWorld();

    @BeforeEach
    void hanaCleaningIsLiveWithItsPayoutAccountButNoHelperYet() {
        world.providers.save(Provider.restore(
                HANA_CLEANING, new Owner(new PersonId(HANA)), ProviderStatus.LIVE, PayoutAccountHolder.OWNER));
        world.verifiedPeople.save(verifiedLocal(HANA, "Hana"));
    }

    private OwnerJoinTeamResponse hanaJoins(String role) {
        return world.joinTeam.join(new OwnerJoinTeamRequest(HANA, HANA_CLEANING.value(), role));
    }

    @Test
    @Tag("PRV-6")
    @DisplayName("Hana is Hana Cleaning's only helper: she joins her own team, and it can be booked again.")
    void ownerJoinsTheirOwnTeamAsTheOnlyHelper() {
        OwnerJoinTeamResponse response = hanaJoins("HELPER");

        assertThat(response.memberId()).isNotBlank();
        assertThat(response.providerId()).isEqualTo(HANA_CLEANING.value());
        assertThat(response.personId()).isEqualTo(HANA);
        assertThat(response.role()).isEqualTo("HELPER");
        assertThat(response.eligibility()).isEqualTo("LOCAL");
        assertThat(response.photoVerified()).isFalse();
        assertThat(response.rosterableUntil()).isNull();
        TeamMember hana = world.members.stored(new TeamMemberId(response.memberId()));
        assertThat(hana.providerId()).isEqualTo(HANA_CLEANING);
        assertThat(hana.personId()).isEqualTo(new PersonId(HANA));
        assertThat(hana.isActiveHelper()).isTrue();
        assertThat(hana.addedAt()).isEqualTo(TEAM_ADDED_AT);
        assertThat(hana.verifiedUntil()).isEqualTo(NEXT_DUE_AT);

        List<ProviderIE> published = world.publisher.published();
        assertThat(published).hasSize(2);
        TeamMemberActivatedIE activated = (TeamMemberActivatedIE) published.get(0);
        assertThat(activated.eventId()).isEqualTo("event-1");
        assertThat(activated.memberId()).isEqualTo(response.memberId());
        assertThat(activated.personId()).isEqualTo(HANA);
        assertThat(activated.providerId()).isEqualTo(HANA_CLEANING.value());
        assertThat(activated.displayName()).isEqualTo("Hana");
        assertThat(activated.role()).isEqualTo("HELPER");
        assertThat(activated.eligibility()).isEqualTo("LOCAL");
        assertThat(activated.photoVerified()).isFalse();
        assertThat(activated.rosterableUntil()).isNull();
        assertThat(activated.occurredAt()).isEqualTo(TEAM_ADDED_AT);
        ProviderBookabilityChangedIE bookability = (ProviderBookabilityChangedIE) published.get(1);
        assertThat(bookability.eventId()).isEqualTo("event-2");
        assertThat(bookability.providerId()).isEqualTo(HANA_CLEANING.value());
        assertThat(bookability.bookable()).isTrue();
        assertThat(bookability.reason()).isNull();
        assertThat(bookability.occurredAt()).isEqualTo(TEAM_ADDED_AT);
    }

    @Test
    @Tag("PRV-6")
    @DisplayName("Hana's photo was verified before she joined, so she starts with a verified photo.")
    void ownerStartsWithTheirVerifiedPhoto() {
        world.photos.record(new PersonId(HANA), true);

        OwnerJoinTeamResponse response = hanaJoins("HELPER");

        assertThat(response.photoVerified()).isTrue();
        assertThat(world.members.stored(new TeamMemberId(response.memberId())).hasVerifiedPhoto()).isTrue();
        assertThat(world.publisher.published().get(0)).isInstanceOfSatisfying(TeamMemberActivatedIE.class,
                event -> assertThat(event.photoVerified()).isTrue());
    }

    @Test
    @Tag("PRV-6")
    @Tag("PRV-3")
    @DisplayName("Hana can also join as a supervisor: she isn't a helper, so Hana Cleaning stays unbookable.")
    void ownerJoinsAsASupervisor() {
        OwnerJoinTeamResponse response = hanaJoins("SUPERVISOR");

        assertThat(response.role()).isEqualTo("SUPERVISOR");
        assertThat(world.members.stored(new TeamMemberId(response.memberId())).isActiveHelper()).isFalse();
        assertThat(world.publisher.published()).hasSize(1).allMatch(TeamMemberActivatedIE.class::isInstance);
    }

    @Test
    @Tag("PRV-6")
    @Tag("HLP-10")
    @DisplayName("Hana is on her own team once; joining again is refused, because a person belongs to one provider.")
    void ownerCantJoinTheirOwnTeamTwice() {
        hanaJoins("HELPER");

        assertThatThrownBy(() -> hanaJoins("SUPERVISOR")).isInstanceOf(PersonAlreadyBelongsToProviderException.class);
        assertThat(world.members.all()).hasSize(1);
        assertThat(world.publisher.published()).hasSize(2);
    }

    @Test
    @Tag("PRV-6")
    @Tag("HLP-8")
    @Tag("HLP-10")
    @DisplayName("Hana stops working in her team and is removed from it; she still owns Hana Cleaning, and can join again as a new member.")
    void ownerRemovedFromTheirTeamCanJoinAgainAsANewMember() {
        OwnerJoinTeamResponse first = hanaJoins("HELPER");
        world.removeTeamMember.remove(new RemoveTeamMemberRequest(HANA, HANA_CLEANING.value(), first.memberId()));

        OwnerJoinTeamResponse second = hanaJoins("SUPERVISOR");

        assertThat(second.memberId()).isNotEqualTo(first.memberId());
        assertThat(world.members.stored(new TeamMemberId(first.memberId())).isRemoved()).isTrue();
        assertThat(world.members.stored(new TeamMemberId(second.memberId())).isActive()).isTrue();
        assertThat(world.providers.stored(HANA_CLEANING).isRunBy(new PersonId(HANA))).isTrue();
    }

    @Test
    @Tag("PRV-1")
    @Tag("PRV-6")
    @DisplayName("Only the owner joins as the owner: Siti, a helper, can't add herself through Hana Cleaning's owner join.")
    void onlyTheOwnerJoins() {
        world.verifiedPeople.save(verifiedLocal(SITI, "Siti"));

        assertThatThrownBy(() -> world.joinTeam.join(new OwnerJoinTeamRequest(SITI, HANA_CLEANING.value(), "HELPER")))
                .isInstanceOf(NotAllowedException.class);
        assertThat(world.members.all()).isEmpty();
        assertThat(world.publisher.published()).isEmpty();
    }

    @Test
    @Tag("IDN-1")
    @DisplayName("An owner Verification hasn't told Provider about can't join their team.")
    void ownerWhoseVerificationIsUnknownIsRefused() {
        world.providers.save(Provider.restore(
                new ProviderId("provider-azman-test"), new Owner(new PersonId("person-unverified")),
                ProviderStatus.OPENED, null));

        assertThatThrownBy(() -> world.joinTeam.join(
                new OwnerJoinTeamRequest("person-unverified", "provider-azman-test", "HELPER")))
                .isInstanceOf(PersonNotVerifiedException.class)
                .hasMessageContaining("person-unverified");
        assertThat(world.members.all()).isEmpty();
    }

    @Test
    @Tag("PRV-6")
    @DisplayName("A role other than HELPER or SUPERVISOR is refused.")
    void unknownRoleIsRefused() {
        assertThatThrownBy(() -> hanaJoins("OWNER")).isInstanceOf(IllegalArgumentException.class);
        assertThat(world.members.all()).isEmpty();
    }

    @Test
    @Tag("PRV-6")
    @DisplayName("Azman doesn't clean, so he isn't in Kemas Home's team; if he did, he could join it as a supervisor.")
    void ownerOfAnotherLiveProviderJoinsOnlyTheirOwn() {
        world.providers.save(ProviderUsecaseFixtures.kemasHome(ProviderStatus.LIVE, PayoutAccountHolder.BUSINESS));
        world.verifiedPeople.save(verifiedLocal(AZMAN, "Azman"));
        world.time.setNow(at(2026, 9, 2, 9, 0));

        OwnerJoinTeamResponse response = world.joinTeam.join(
                new OwnerJoinTeamRequest(AZMAN, KEMAS_HOME.value(), "SUPERVISOR"));

        assertThat(response.providerId()).isEqualTo(KEMAS_HOME.value());
        assertThat(world.members.stored(new TeamMemberId(response.memberId())).addedAt())
                .isEqualTo(at(2026, 9, 2, 9, 0));
    }
}
