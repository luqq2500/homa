package verification.domain;

import java.util.UUID;

/** Generates the id of a team \1ember. */
public final class TeamMemberId {

    public String generate() {
        return UUID.randomUUID().toString();
    }
}
