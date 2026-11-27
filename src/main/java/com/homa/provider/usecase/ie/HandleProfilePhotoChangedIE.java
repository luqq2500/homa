package com.homa.provider.usecase.ie;

import com.homa.ie.events.ProfilePhotoChangedIE;
import com.homa.provider.domain.PersonId;
import com.homa.provider.domain.TeamMember;
import com.homa.provider.usecase.port.in.ProfilePhotoChangedIEHandler;
import com.homa.provider.usecase.port.out.ProcessedMessageStore;
import com.homa.provider.usecase.port.out.ProfilePhotoReplica;
import com.homa.provider.usecase.port.out.TeamMemberRepository;
import java.util.Objects;

/**
 * Takes Verification's latest word on a person's face photo (IDN-4). The member who is that person records it; only
 * a member with a verified photo can be rostered (ROS-6). The fact is also kept per person, so someone whose photo is
 * verified before they are added starts with it. Provider publishes nothing: Reservation consumes
 * {@code ProfilePhotoChangedIE} itself.
 */
public final class HandleProfilePhotoChangedIE implements ProfilePhotoChangedIEHandler {

    static final String HANDLER = "provider.ProfilePhotoChanged";

    private final ProcessedMessageStore processedMessages;
    private final ProfilePhotoReplica photos;
    private final TeamMemberRepository members;

    public HandleProfilePhotoChangedIE(ProcessedMessageStore processedMessages, ProfilePhotoReplica photos,
            TeamMemberRepository members) {
        this.processedMessages = Objects.requireNonNull(processedMessages, "processedMessages");
        this.photos = Objects.requireNonNull(photos, "photos");
        this.members = Objects.requireNonNull(members, "members");
    }

    @Override
    public void handle(ProfilePhotoChangedIE event) {
        if (!processedMessages.recordFirstHandling(event.eventId(), HANDLER)) {
            return;
        }
        PersonId personId = new PersonId(event.personId());
        photos.record(personId, event.verified());
        for (TeamMember member : members.findByPersonId(personId)) {
            member.recordPhotoVerification(event.verified());
            members.save(member);
        }
    }
}
