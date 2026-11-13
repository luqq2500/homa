package reservation.usecase.dto;

import java.util.List;

public record DisputeMissedCompletionsResponse(List<String> ids) {

    public DisputeMissedCompletionsResponse {
        ids = List.copyOf(ids);
    }
}
