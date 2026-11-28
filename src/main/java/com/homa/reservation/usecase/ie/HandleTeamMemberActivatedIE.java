package com.homa.reservation.usecase.ie;

import com.homa.ie.events.TeamMemberActivatedIE;
import com.homa.reservation.domain.replica.PersonPhoto;
import com.homa.reservation.domain.replica.RosterableMember;
import com.homa.reservation.domain.roster.MemberRole;
import com.homa.reservation.usecase.port.in.TeamMemberActivatedIEHandler;
import com.homa.reservation.usecase.port.out.PersonPhotoRepository;
import com.homa.reservation.usecase.port.out.ProcessedMessageStore;
import com.homa.reservation.usecase.port.out.RosterableMemberRepository;
import java.time.LocalDate;
import java.util.Optional;

/**
 * ROS-3, ROS-4, ROS-6: a helper or supervisor is active on the provider's team, under their verified name. Photo
 * checks are ordered by person and activations by member, so either can come first: a verified photo Verification
 * already reported is applied now and wins over Provider's {@code photoVerified} flag; without one, the flag stands
 * and the member waits for the photo before they can be rostered (IDN-4). A foreign member's last job date is
 * Provider's, already net of its permit buffer.
 */
public final class HandleTeamMemberActivatedIE implements TeamMemberActivatedIEHandler {

    static final String HANDLER = "reservation.TeamMemberActivated";

    private final ProcessedMessageStore processed;
    private final RosterableMemberRepository members;
    private final PersonPhotoRepository photos;

    public HandleTeamMemberActivatedIE(ProcessedMessageStore processed, RosterableMemberRepository members,
            PersonPhotoRepository photos) {
        this.processed = processed;
        this.members = members;
        this.photos = photos;
    }

    @Override
    public void handle(TeamMemberActivatedIE event) {
        if (!processed.recordFirstHandling(event.eventId(), HANDLER)) {
            return;
        }
        PersonPhoto photo = photos.findByPersonId(event.personId())
                .orElseGet(() -> PersonPhoto.none(event.personId()));
        members.save(new RosterableMember(
                event.memberId(),
                event.personId(),
                event.providerId(),
                MemberRole.valueOf(event.role()),
                event.displayName(),
                event.photoVerified(),
                Optional.empty(),
                true,
                Optional.ofNullable(event.rosterableUntil()).map(LocalDate::parse)).showing(photo));
    }
}
