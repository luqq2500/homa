package com.homa.reservation.usecase.ie;

import com.homa.ie.events.TeamMemberDeactivatedIE;
import com.homa.reservation.usecase.RosterReplacements;
import com.homa.reservation.usecase.port.in.TeamMemberDeactivatedIEHandler;
import com.homa.reservation.usecase.port.out.ProcessedMessageStore;
import com.homa.reservation.usecase.port.out.RosterableMemberRepository;

/**
 * HLP-6, ROS-5: the provider deactivated or removed a member. They can't be rostered from now on, and every upcoming
 * roster they are on is flagged to be fixed, with the provider told through a published event, so it replaces them
 * before the Check-in Period starts. A roster whose Check-in Period has started is locked and left alone. The
 * member's reactivation comes as an activation, which makes them rosterable again.
 */
public final class HandleTeamMemberDeactivatedIE implements TeamMemberDeactivatedIEHandler {

    static final String HANDLER = "reservation.TeamMemberDeactivated";

    private final ProcessedMessageStore processed;
    private final RosterableMemberRepository members;
    private final RosterReplacements replacements;

    public HandleTeamMemberDeactivatedIE(ProcessedMessageStore processed, RosterableMemberRepository members,
            RosterReplacements replacements) {
        this.processed = processed;
        this.members = members;
        this.replacements = replacements;
    }

    @Override
    public void handle(TeamMemberDeactivatedIE event) {
        if (!processed.recordFirstHandling(event.eventId(), HANDLER)) {
            return;
        }
        members.findById(event.memberId()).ifPresent(member -> members.save(member.deactivated()));
        replacements.flagRostersWith(event.memberId());
    }
}
