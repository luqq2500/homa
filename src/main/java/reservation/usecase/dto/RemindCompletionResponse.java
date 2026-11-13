package reservation.usecase.dto;

import java.util.List;

public record RemindCompletionResponse(List<String> ids) {

    public RemindCompletionResponse {
        ids = List.copyOf(ids);
    }
}
