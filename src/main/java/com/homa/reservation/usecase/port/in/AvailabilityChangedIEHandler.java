package com.homa.reservation.usecase.port.in;

import com.homa.ie.events.AvailabilityChangedIE;

/** RES-3, OFR-8: the provider's weekly working hours. */
public interface AvailabilityChangedIEHandler {

    void handle(AvailabilityChangedIE event);
}
