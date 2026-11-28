package com.homa.provider.domain;

import com.homa.kernel.TimeInterval;
import java.time.Instant;
import java.util.Collection;
import java.util.List;

/**
 * One member's rostered windows: for each reservation they are rostered on, the period from the start of its Check-in
 * Period to its job end (HLP-9). It is a replica, kept from Reservation's events through {@link RosteredReservation};
 * a member is checked against it when they are to be removed.
 *
 * <p>A window is half-open, as {@link TimeInterval} is: it includes the moment the Check-in Period starts and
 * excludes the job end, so a member whose job ends at 14:00 can be removed at 14:00.
 */
public record RosteredWindows(List<TimeInterval> windows) {

    public RosteredWindows {
        windows = List.copyOf(windows);
    }

    public static RosteredWindows of(Collection<TimeInterval> windows) {
        return new RosteredWindows(List.copyOf(windows));
    }

    /** The windows of a member who is rostered on nothing. */
    public static RosteredWindows none() {
        return new RosteredWindows(List.of());
    }

    /** True when {@code instant} is in the Check-in Period or job time of any of them (HLP-9). */
    public boolean isInsideOneAt(Instant instant) {
        return windows.stream().anyMatch(window -> window.contains(instant));
    }
}
