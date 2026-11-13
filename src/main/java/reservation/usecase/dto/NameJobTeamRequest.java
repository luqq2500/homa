package reservation.usecase.dto;

import java.util.Set;

public record NameJobTeamRequest(String providerId, String reservationId, Set<String> memberIds) {

    public NameJobTeamRequest {
        memberIds = Set.copyOf(memberIds);
    }
}
