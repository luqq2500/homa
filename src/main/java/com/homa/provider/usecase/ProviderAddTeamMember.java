package com.homa.provider.usecase;

import com.homa.provider.domain.Bookability;
import com.homa.provider.domain.PersonId;
import com.homa.provider.domain.Provider;
import com.homa.provider.domain.TeamMember;
import com.homa.provider.domain.TeamMemberId;
import com.homa.provider.domain.TeamRole;
import com.homa.provider.usecase.dto.AddTeamMemberRequest;
import com.homa.provider.usecase.dto.AddTeamMemberResponse;
import com.homa.provider.usecase.exception.PersonNotVerifiedException;
import com.homa.provider.usecase.port.in.TeamMemberAdder;
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
 * The owner adds a verified person to the provider's team, before or after going live (HLP-1 to HLP-4, PRV-3), from
 * what Verification decided about them. The person must belong to no provider yet (HLP-10): the use case looks them up
 * and passes what it found to the domain, which refuses. The member starts active, with the photo verification
 * Verification last reported for them (IDN-4), and {@code TeamMemberActivatedIE} tells Reservation and Listing. A live
 * provider with no active helper becomes bookable again when a helper joins, so bookability is re-checked (PRV-2).
 */
public final class ProviderAddTeamMember implements TeamMemberAdder {

    private final ProviderRepository providers;
    private final TeamMemberRepository members;
    private final VerifiedPersonReplica verifiedPeople;
    private final AffiliationLookup affiliations;
    private final ProfilePhotoReplica photos;
    private final ProviderIEPublisher publisher;
    private final TimeSource time;
    private final EventIds eventIds;
    private final TeamBookability bookability;

    public ProviderAddTeamMember(ProviderRepository providers, TeamMemberRepository members,
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
    public AddTeamMemberResponse add(AddTeamMemberRequest request) {
        Provider provider = OwnerAuthorization.providerRunBy(providers, request.providerId(), request.actingPersonId());
        PersonId personId = new PersonId(request.personId());
        VerifiedPerson person = verifiedPeople.findByPersonId(personId)
                .orElseThrow(() -> new PersonNotVerifiedException(personId.value()));
        TeamRole role = TeamRole.valueOf(request.role());
        Instant now = time.now();
        Bookability before = bookability.of(provider);
        TeamMember member = provider.addTeamMember(TeamMemberId.generate(), role, person.facts(),
                request.employmentConfirmed(), affiliations.of(personId), now);
        photos.photoVerifiedOf(personId).ifPresent(member::recordPhotoVerification);
        members.save(member);
        publisher.publish(TeamMemberEvents.activated(eventIds.next(), member, person.name(), now));
        bookability.announceChangeFrom(before, provider, now);
        return new AddTeamMemberResponse(member.id().value(), member.providerId().value(), member.personId().value(),
                member.role().name(), TeamMemberEvents.eligibilityOf(member), member.hasVerifiedPhoto(),
                member.rosterableUntil().orElse(null));
    }
}
