package com.homa.listing.usecase.port.in;

import com.homa.ie.events.TeamMemberActivatedIE;

/** Counts an active helper towards the default maximum of helpers (OFR-5). */
public interface TeamMemberActivatedIEHandler {

    void handle(TeamMemberActivatedIE event);
}
