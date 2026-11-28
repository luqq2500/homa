package com.homa.listing.usecase.port.in;

import com.homa.ie.events.TeamMemberDeactivatedIE;

/** Stops counting a deactivated or removed member as an active helper (OFR-5). */
public interface TeamMemberDeactivatedIEHandler {

    void handle(TeamMemberDeactivatedIE event);
}
