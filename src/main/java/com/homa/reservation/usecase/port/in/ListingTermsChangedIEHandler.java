package com.homa.reservation.usecase.port.in;

import com.homa.ie.events.ListingTermsChangedIE;

/** RES-8: the provider's terms for new reservations. */
public interface ListingTermsChangedIEHandler {

    void handle(ListingTermsChangedIE event);
}
