package com.homa.provider.usecase;

import com.homa.provider.domain.Bookability;
import com.homa.provider.domain.PersonId;
import com.homa.provider.domain.Provider;
import com.homa.provider.domain.TeamMember;
import com.homa.provider.domain.TeamMemberId;
import com.homa.provider.domain.TeamRole;
import com.homa.provider.usecase.dto.OwnerJoinTeamRequest;
import com.homa.provider.usecase.dto.OwnerJoinTeamResponse;
import com.homa.provider.usecase.exception.PersonNotVerifiedException;
import com.homa.provider.usecase.port.in.OwnerTeamJoiner;
import com.homa.provider.usecase.port.out.AffiliationLookup;
import com.homa.provider.usecase.port.out.EventIds;
import com.homa.provider.usecase.port.out.ProfilePhotoReplica;
import com.homa.provider.usecase.port.out.ProviderIEPublisher;
import com.homa.provider.usecase.port.out.ProviderRepository;
import com.homa.provider.usecase.port.out.TeamMemberRepository;
import com.homa.provider.usecase.port.out.TimeSource;
import com.homa.provider.usecase.port.out.VerifiedPerson;
import com.homa.provider.usecase.port.out.VerifiedPersonReplica;
import java.time.Instant;
import java.util.Objects;

/**
 * The owner also works in their own team as a helper or supervisor (PRV-6): Hana is Hana Cleaning's only helper. The
 * owner is the same person, so HLP-10 allows it, once; the domain judges them from what Verification decided about
 * them, with the checks that apply to a local member (HLP-1, HLP-2). The member starts active, with the photo
 * verification Verification last reported for them (IDN-4), and {@code TeamMemberActivatedIE} tells Reservation and
 * Listing. A live provider with no active helper becomes bookable again when its owner joins as a helper, so
 * bookability is re-checked (PRV-2).
 */
public final class OwnerJoinTeam implements OwnerTeamJoiner {

    private final ProviderRepository providers;
    private final TeamMemberRepository members;
    private final VerifiedPersonReplica verifiedPeople;
    private final AffiliationLookup affiliations;
    private final ProfilePhotoReplica photos;
    private final ProviderIEPublisher publisher;
    private final TimeSource time;
    private final EventIds eventIds;
    private final TeamBookability bookability;

    public OwnerJoinTeam(ProviderRepository providers, TeamMemberRepository members,
            VerifiedPersonReplica verifiedPeople, AffiliationLookup affiliations, ProfilePhotoReplica photos,
            ProviderIEPublisher publisher, TimeSource time, EventIds eventIds) {
        this.providers = Objects.requireNonNull(providers, "providers");
        this.members = Objects.requireNonNull(members, "members");
        this.verifiedPeople = Objects.requireNonNull(verifiedPeople, "verifiedPeople");
        this.affiliations = Objects.requireNonNull(affiliations, "affiliations");
        this.photos = Objects.requireNonNull(photos, "photos");
        this.publisher = Objects.requireNonNull(publisher, "publisher");
        this.time = Objects.requireNonNull(time, "time");
        this.eventIds = Objects.requireNonNull(eventIds, "eventIds");
        this.bookability = new TeamBookability(members, publisher, eventIds);
    }

    @Override
    public OwnerJoinTeamResponse join(OwnerJoinTeamRequest request) {
        Provider provider = OwnerAuthorization.providerRunBy(providers, request.providerId(), request.actingPersonId());
        PersonId owner = new PersonId(request.actingPersonId());
        VerifiedPerson person = verifiedPeople.findByPersonId(owner)
                .orElseThrow(() -> new PersonNotVerifiedException(owner.value()));
        TeamRole role = TeamRole.valueOf(request.role());
        Instant now = time.now();
        Bookability before = bookability.of(provider);
        TeamMember member = provider.addOwnerToTeam(
                TeamMemberId.generate(), role, person.facts(), affiliations.of(owner), now);
        photos.photoVerifiedOf(owner).ifPresent(member::recordPhotoVerification);
        members.save(member);
        publisher.publish(TeamMemberEvents.activated(eventIds.next(), member, person.name(), now));
        bookability.announceChangeFrom(before, provider, now);
        return new OwnerJoinTeamResponse(member.id().value(), member.providerId().value(), member.personId().value(),
                member.role().name(), TeamMemberEvents.eligibilityOf(member), member.hasVerifiedPhoto(),
                member.rosterableUntil().orElse(null));
    }
}
