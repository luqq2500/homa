package com.homa.ie.events;

import com.homa.ie.port.VerificationIE;
import java.time.Instant;

/**
 * An owner's or member's face photo was checked against their verified identity (IDN-4). Only a member with a
 * verified photo can be rostered, and the customer sees it on the roster (ROS-6). A new photo replaces the current
 * one only once verified, so {@code photoRef} is always the verified photo.
 */
public record ProfilePhotoChangedIE(
        String eventId, String personId, boolean verified, String photoRef, Instant occurredAt)
        implements VerificationIE {
}
