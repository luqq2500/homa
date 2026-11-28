package com.homa.reservation.domain.replica;

import com.homa.reservation.domain.roster.MemberRole;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * What Reservation has heard about a provider so far, from Listing's and Provider's events. They arrive in no
 * particular order across the two contexts, so each event changes only its own part. The provider can be booked
 * through a {@link BookableProvider} only once its listing is known.
 *
 * @param ownerPersonId PRV-1, PRV-11: the owner, from {@code ProviderOpenedIE}; empty until Provider has said who it is
 * @param bookable     PRV-2: true once the provider went live, and while it stays bookable
 * @param listing      Listing's latest snapshot; empty until Listing has published one
 * @param workingHours the weekly working hours (OFR-8); none are worked until Listing publishes them (RES-3)
 */
public record ProviderReplica(
        String providerId,
        Optional<String> ownerPersonId,
        boolean bookable,
        Optional<ProviderListing> listing,
        WorkingHours workingHours) {

    public ProviderReplica {
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(ownerPersonId, "ownerPersonId");
        Objects.requireNonNull(listing, "listing");
        Objects.requireNonNull(workingHours, "workingHours");
    }

    /** A provider Reservation has heard nothing else about yet: not bookable, not listed, and working no hours. */
    public static ProviderReplica unheardOf(String providerId) {
        return new ProviderReplica(providerId, Optional.empty(), false, Optional.empty(), new WorkingHours(Map.of()));
    }

    /** PRV-1, PRV-11: a verified person opened the provider as its owner; they act on all its reservations. */
    public ProviderReplica ownedBy(String ownerPerson) {
        return new ProviderReplica(providerId, Optional.of(ownerPerson), bookable, listing, workingHours);
    }

    /** Listing published the provider's terms anew; only new reservations use them (RES-8). */
    public ProviderReplica listedAs(ProviderListing newListing) {
        return new ProviderReplica(providerId, ownerPersonId, bookable, Optional.of(newListing), workingHours);
    }

    /** Listing published the provider's weekly working hours anew (OFR-8, RES-3). */
    public ProviderReplica working(WorkingHours newHours) {
        return new ProviderReplica(providerId, ownerPersonId, bookable, listing, newHours);
    }

    /** PRV-1, PRV-2: the provider went live, became bookable again or stopped being bookable. */
    public ProviderReplica withBookability(boolean nowBookable) {
        return new ProviderReplica(providerId, ownerPersonId, nowBookable, listing, workingHours);
    }

    /**
     * The provider as its booking rules see it, once it is listed and its owner is known (PRV-1). Customers can book at most the smaller of the
     * maximum helpers and the provider's active helpers (OFR-5), so the active helpers are counted from its team.
     * External busy times arrive with calendar sync and are none until then (OFR-8).
     */
    public Optional<BookableProvider> asBookable(List<RosterableMember> team) {
        int activeHelpers = (int) team.stream().filter(this::isActiveHelper).count();
        return listing.flatMap(known -> ownerPersonId.map(owner -> new BookableProvider(providerId, owner, bookable,
                known.cities(), known.jobs(), known.terms(), known.bounds(), activeHelpers, known.minimumNotice(),
                known.buffer(), workingHours, List.of())));
    }

    private boolean isActiveHelper(RosterableMember member) {
        return member.worksFor(providerId) && member.active() && member.role() == MemberRole.HELPER;
    }
}
