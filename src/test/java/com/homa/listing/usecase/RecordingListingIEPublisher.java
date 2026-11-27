package com.homa.listing.usecase;

import com.homa.ie.port.ListingIE;
import com.homa.listing.usecase.port.out.ListingIEPublisher;
import java.util.ArrayList;
import java.util.List;

final class RecordingListingIEPublisher implements ListingIEPublisher {

    private final List<ListingIE> published = new ArrayList<>();

    @Override
    public void publish(ListingIE event) {
        published.add(event);
    }

    List<ListingIE> published() {
        return List.copyOf(published);
    }

    /** The one event published since the last clear, as its type. */
    <T extends ListingIE> T single(Class<T> type) {
        if (published.size() != 1) {
            throw new AssertionError("Expected one event, was " + published);
        }
        return type.cast(published.get(0));
    }

    void clear() {
        published.clear();
    }
}
