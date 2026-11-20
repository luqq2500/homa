package com.homa.provider.domain;

import java.util.List;

/** A provider's helpers and supervisors. Only active helpers count towards going live and being booked (PRV-2, PRV-3). */
public record Team(List<TeamMember> members) {

    public Team {
        members = List.copyOf(members);
    }

    /** The number of active helpers. Supervisors are optional and never count (PRV-3). */
    public int activeHelperCount() {
        return (int) members.stream().filter(TeamMember::isActiveHelper).count();
    }
}
