package com.homa.listing.domain;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * What Listing knows of a provider from Provider's events: whether it is live, whether it can be booked, and which
 * helpers are active. A replica, so it exists before the provider has a listing, and every change to it is
 * idempotent.
 *
 * <ul>
 *   <li>A provider shows in search only while it is live and bookable (PRV-2, RES-1).</li>
 *   <li>The active helpers are the default maximum of helpers a customer can book (OFR-5); supervisors don't
 *       count, since they are extra and not billed (ROS-2).</li>
 * </ul>
 */
public final class ProviderStanding {

    private final ProviderId providerId;
    private boolean live;
    private boolean bookable;
    private final Set<String> activeHelperIds;

    private ProviderStanding(ProviderId providerId, boolean live, boolean bookable, Set<String> activeHelperIds) {
        this.providerId = Objects.requireNonNull(providerId, "providerId");
        this.live = live;
        this.bookable = bookable;
        this.activeHelperIds = new LinkedHashSet<>(activeHelperIds);
    }

    /** A provider Listing has heard nothing about yet: not live, not bookable, no active helpers. */
    public static ProviderStanding unknown(ProviderId providerId) {
        return new ProviderStanding(providerId, false, false, Set.of());
    }

    /** The standing as it was stored. */
    public static ProviderStanding rebuild(ProviderId providerId, boolean live, boolean bookable,
                                           Set<String> activeHelperIds) {
        return new ProviderStanding(providerId, live, bookable, activeHelperIds);
    }

    public ProviderId providerId() {
        return providerId;
    }

    public boolean live() {
        return live;
    }

    public boolean bookable() {
        return bookable;
    }

    public Set<String> activeHelperIds() {
        return Set.copyOf(activeHelperIds);
    }

    /** A provider goes live only when it can be booked (PRV-2), so it is live and bookable from now on. */
    public void wentLive() {
        live = true;
        bookable = true;
    }

    /** The provider became bookable, or stopped being bookable (PRV-2). */
    public void changeBookability(boolean nowBookable) {
        bookable = nowBookable;
    }

    /** True while the provider shows to customers: live and bookable (PRV-2). */
    public boolean isListed() {
        return live && bookable;
    }

    /**
     * A member became active. A helper joins the active helpers; a supervisor changes nothing (ROS-2).
     *
     * @return true when the number of active helpers changed, so the default maximum of helpers did too (OFR-5)
     */
    public boolean memberActivated(String memberId, MemberRole role) {
        Objects.requireNonNull(memberId, "memberId");
        return role == MemberRole.HELPER && activeHelperIds.add(memberId);
    }

    /** The number of active helpers: the default maximum of helpers a customer can book (OFR-5). */
    public int activeHelperCount() {
        return activeHelperIds.size();
    }
}
