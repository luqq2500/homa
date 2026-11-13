package reservation.usecase.port.out;

import java.util.List;
import java.util.Optional;
import reservation.domain.TeamRoster;

public interface TeamRosterRepository {

    void save(TeamRoster teamRoster);

    Optional<TeamRoster> find(String id);

    List<TeamRoster> all();
}
