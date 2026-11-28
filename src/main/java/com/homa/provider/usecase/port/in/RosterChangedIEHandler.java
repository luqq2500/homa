package com.homa.provider.usecase.port.in;

import com.homa.ie.events.RosterChangedIE;

/** Keeps who is rostered on a reservation, so each member has their windows (HLP-9). */
public interface RosterChangedIEHandler {

    void handle(RosterChangedIE event);
}
