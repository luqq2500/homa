package com.homa.provider.usecase;

import com.homa.ie.events.TeamMemberActivatedIE;
import com.homa.provider.domain.PersonId;
import com.homa.provider.domain.Provider;
import com.homa.provider.domain.TeamMember;
import com.homa.provider.domain.TeamMemberId;
import com.homa.provider.domain.TeamRole;
import com.homa.provider.usecase.dto.AddTeamMemberRequest;
import com.homa.provider.usecase.dto.AddTeamMemberResponse;
import com.homa.provider.usecase.exception.PersonNotVerifiedException;
import com.homa.provider.usecase.port.in.TeamMemberAdder;
import com.homa.provider.usecase.port.out.EventIds;
import com.homa.provider.usecase.port.out.ProfilePhotoReplica;
import com.homa.provider.usecase.port.out.ProviderIEPublisher;
import com.homa.provider.usecase.port.out.ProviderRepository;
import com.homa.provider.usecase.port.out.TeamMemberRepository;
import com.homa.provider.usecase.port.out.TimeSource;
import com.homa.provider.usecase.port.out.VerifiedPerson;
import com.homa.provider.usecase.port.out.VerifiedPersonReplica;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/**
 * The owner adds a verified person to the provider's team, before or after going live (HLP-1 to HLP-4, PRV-3), from
 * what Verification decided about them. The member starts active, with the photo verification Verification last
 * reported for them (IDN-4), and {@code TeamMemberActivatedIE} tells Reservation and Listing.
 *
 * <p>Hook for slice 4: before {@link Provider#addTeamMember}, look up the person's {@code Affiliation} and pass it in,
 * so a person who already owns or belongs to a provider is refused (HLP-10).
 */
public final class ProviderAddTeamMember implements TeamMemberAdder {

    static final String LOCAL = "LOCAL";
    static final String FOREIGN_WORKER = "FOREIGN_WORKER";

    private final ProviderRepository providers;
    private final TeamMemberRepository members;
    private final VerifiedPersonReplica verifiedPeople;
    private final ProfilePhotoReplica photos;
    private final ProviderIEPublisher publisher;
    private final TimeSource time;
    private final EventIds eventIds;

    public ProviderAddTeamMember(ProviderRepository providers, TeamMemberRepository members,
            VerifiedPersonReplica verifiedPeople, ProfilePhotoReplica photos, ProviderIEPublisher publisher,
            TimeSource time, EventIds eventIds) {
        this.providers = Objects.requireNonNull(providers, "providers");
        this.members = Objects.requireNonNull(members, "members");
        this.verifiedPeople = Objects.requireNonNull(verifiedPeople, "verifiedPeople");
        this.photos = Objects.requireNonNull(photos, "photos");
        this.publisher = Objects.requireNonNull(publisher, "publisher");
        this.time = Objects.requireNonNull(time, "time");
        this.eventIds = Objects.requireNonNull(eventIds, "eventIds");
    }

    @Override
    public AddTeamMemberResponse add(AddTeamMemberRequest request) {
        Provider provider = OwnerAuthorization.providerRunBy(providers, request.providerId(), request.actingPersonId());
        PersonId personId = new PersonId(request.personId());
        VerifiedPerson person = verifiedPeople.findByPersonId(personId)
                .orElseThrow(() -> new PersonNotVerifiedException(personId.value()));
        TeamRole role = TeamRole.valueOf(request.role());
        Instant now = time.now();
        TeamMember member = provider.addTeamMember(
                TeamMemberId.generate(), role, person.facts(), request.employmentConfirmed(), now);
        photos.photoVerifiedOf(personId).ifPresent(member::recordPhotoVerification);
        members.save(member);
        publisher.publish(activated(member, person.name(), now));
        return new AddTeamMemberResponse(member.id().value(), member.providerId().value(), member.personId().value(),
                member.role().name(), eligibilityOf(member), member.hasVerifiedPhoto(),
                member.rosterableUntil().orElse(null));
    }

    private TeamMemberActivatedIE activated(TeamMember member, String displayName, Instant at) {
        return new TeamMemberActivatedIE(
                eventIds.next(),
                member.id().value(),
                member.personId().value(),
                member.providerId().value(),
                displayName,
                member.role().name(),
                eligibilityOf(member),
                member.hasVerifiedPhoto(),
                member.rosterableUntil().map(LocalDate::toString).orElse(null),
                at);
    }

    private static String eligibilityOf(TeamMember member) {
        return member.eligibility().isLocal() ? LOCAL : FOREIGN_WORKER;
    }
}
