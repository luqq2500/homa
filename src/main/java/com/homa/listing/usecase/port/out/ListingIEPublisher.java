package com.homa.listing.usecase.port.out;

import com.homa.ie.port.ListingIE;

/** The Listing context's one integration event publisher, called inside the usecase's transaction. */
public interface ListingIEPublisher {

    void publish(ListingIE event);
}
