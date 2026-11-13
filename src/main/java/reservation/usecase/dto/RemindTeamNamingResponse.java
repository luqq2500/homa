package reservation.usecase.dto;

import java.util.List;

public record RemindTeamNamingResponse(List<String> ids) {

    public RemindTeamNamingResponse {
        ids = List.copyOf(ids);
    }
}
