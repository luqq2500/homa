package com.homa.listing.domain;

/**
 * A team member's role as Provider names it. Only helpers do the jobs and are billed, so only they count towards
 * the helpers a customer can book (OFR-5); supervisors are extra (ROS-2).
 */
public enum MemberRole {
    HELPER,
    SUPERVISOR
}
