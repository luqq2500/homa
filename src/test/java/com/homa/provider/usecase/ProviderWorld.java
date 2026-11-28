package com.homa.provider.usecase;

import static com.homa.provider.usecase.ProviderUsecaseFixtures.TEAM_ADDED_AT;

import com.homa.provider.usecase.ie.HandlePayoutAccountVerifiedIE;
import com.homa.provider.usecase.ie.HandlePersonVerifiedIE;
import com.homa.provider.usecase.ie.HandleProfilePhotoChangedIE;
import com.homa.provider.usecase.ie.HandleReservationCancelledIE;
import com.homa.provider.usecase.ie.HandleReservationCompletedIE;
import com.homa.provider.usecase.ie.HandleReservationReservedIE;
import com.homa.provider.usecase.ie.HandleRosterChangedIE;

/** Provider's use cases and handlers, wired to in-memory fakes the way {@code app} wires them to infra. */
public final class ProviderWorld {

    public final InMemoryProviderRepository providers = new InMemoryProviderRepository();
    public final InMemoryTeamMemberRepository members = new InMemoryTeamMemberRepository();
    public final InMemoryVerifiedPersonReplica verifiedPeople = new InMemoryVerifiedPersonReplica();
    public final InMemoryProfilePhotoReplica photos = new InMemoryProfilePhotoReplica();
    public final InMemoryAffiliationLookup affiliations = new InMemoryAffiliationLookup(providers, members);
    public final InMemoryRosteredWindowsRepository rosteredWindows = new InMemoryRosteredWindowsRepository();
    public final InMemoryProcessedMessageStore processedMessages = new InMemoryProcessedMessageStore();
    public final RecordingProviderIEPublisher publisher = new RecordingProviderIEPublisher();
    public final FixedTimeSource time = new FixedTimeSource(TEAM_ADDED_AT);
    public final SequentialEventIds eventIds = new SequentialEventIds();

    public final OwnerOpenProvider openProvider = new OwnerOpenProvider(verifiedPeople, providers, affiliations,
            publisher, time, eventIds);
    public final ProviderAddTeamMember addTeamMember = new ProviderAddTeamMember(
            providers, members, verifiedPeople, affiliations, photos, publisher, time, eventIds);
    public final OwnerJoinTeam joinTeam = new OwnerJoinTeam(
            providers, members, verifiedPeople, affiliations, photos, publisher, time, eventIds);
    public final ProviderGoLive goLive = new ProviderGoLive(providers, members, publisher, time, eventIds);
    public final ProviderDeactivateTeamMember deactivateTeamMember = new ProviderDeactivateTeamMember(
            providers, members, publisher, time, eventIds);
    public final ProviderReactivateTeamMember reactivateTeamMember = new ProviderReactivateTeamMember(
            providers, members, verifiedPeople, publisher, time, eventIds);
    public final ProviderRemoveTeamMember removeTeamMember = new ProviderRemoveTeamMember(
            providers, members, rosteredWindows, publisher, time, eventIds);

    public final HandlePersonVerifiedIE personVerified = new HandlePersonVerifiedIE(
            processedMessages, verifiedPeople, members);
    public final HandlePayoutAccountVerifiedIE payoutAccountVerified = new HandlePayoutAccountVerifiedIE(
            processedMessages, providers, members, publisher, time, eventIds);
    public final HandleProfilePhotoChangedIE profilePhotoChanged = new HandleProfilePhotoChangedIE(
            processedMessages, photos, members);
    public final HandleReservationReservedIE reservationReserved = new HandleReservationReservedIE(
            processedMessages, rosteredWindows);
    public final HandleRosterChangedIE rosterChanged = new HandleRosterChangedIE(processedMessages, rosteredWindows);
    public final HandleReservationCompletedIE reservationCompleted = new HandleReservationCompletedIE(
            processedMessages, rosteredWindows);
    public final HandleReservationCancelledIE reservationCancelled = new HandleReservationCancelledIE(
            processedMessages, rosteredWindows);
}
