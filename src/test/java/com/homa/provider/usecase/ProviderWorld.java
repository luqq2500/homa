package com.homa.provider.usecase;

import static com.homa.provider.usecase.ProviderUsecaseFixtures.TEAM_ADDED_AT;

import com.homa.provider.usecase.ie.HandlePayoutAccountVerifiedIE;
import com.homa.provider.usecase.ie.HandlePersonVerifiedIE;
import com.homa.provider.usecase.ie.HandleProfilePhotoChangedIE;

/** Provider's use cases and handlers, wired to in-memory fakes the way {@code app} wires them to infra. */
public final class ProviderWorld {

    public final InMemoryProviderRepository providers = new InMemoryProviderRepository();
    public final InMemoryTeamMemberRepository members = new InMemoryTeamMemberRepository();
    public final InMemoryVerifiedPersonReplica verifiedPeople = new InMemoryVerifiedPersonReplica();
    public final InMemoryProfilePhotoReplica photos = new InMemoryProfilePhotoReplica();
    public final InMemoryProcessedMessageStore processedMessages = new InMemoryProcessedMessageStore();
    public final RecordingProviderIEPublisher publisher = new RecordingProviderIEPublisher();
    public final FixedTimeSource time = new FixedTimeSource(TEAM_ADDED_AT);
    public final SequentialEventIds eventIds = new SequentialEventIds();

    public final OwnerOpenProvider openProvider = new OwnerOpenProvider(verifiedPeople, providers);
    public final ProviderAddTeamMember addTeamMember = new ProviderAddTeamMember(
            providers, members, verifiedPeople, photos, publisher, time, eventIds);
    public final ProviderGoLive goLive = new ProviderGoLive(providers, members, publisher, time, eventIds);

    public final HandlePersonVerifiedIE personVerified = new HandlePersonVerifiedIE(processedMessages, verifiedPeople);
    public final HandlePayoutAccountVerifiedIE payoutAccountVerified = new HandlePayoutAccountVerifiedIE(
            processedMessages, providers, members, publisher, time, eventIds);
    public final HandleProfilePhotoChangedIE profilePhotoChanged = new HandleProfilePhotoChangedIE(
            processedMessages, photos, members);
}
