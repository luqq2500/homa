package account.domain;

import java.util.UUID;

/** Type-safe generator for TeamMember ids. */
public final class TeamMemberId {

    public String generate() {
        return UUID.randomUUID().toString();
    }
}
