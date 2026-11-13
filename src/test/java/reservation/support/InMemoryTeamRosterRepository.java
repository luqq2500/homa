package reservation.support;

import reservation.domain.TeamRoster;
import reservation.usecase.port.out.TeamRosterRepository;
import support.InMemoryStore;

public final class InMemoryTeamRosterRepository extends InMemoryStore<TeamRoster> implements TeamRosterRepository {

    public InMemoryTeamRosterRepository() {
        super(TeamRoster::providerId);
    }
}
