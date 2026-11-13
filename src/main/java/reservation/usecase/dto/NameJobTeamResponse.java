package reservation.usecase.dto;

import java.util.Set;

public record NameJobTeamResponse(String reservationId, Set<String> teamMemberIds) {

    public NameJobTeamResponse {
        teamMemberIds = Set.copyOf(teamMemberIds);
    }
}
