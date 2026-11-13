package reservation.usecase.dto;

import java.util.List;

public record CompleteAfterDisputeWindowResponse(List<String> ids) {

    public CompleteAfterDisputeWindowResponse {
        ids = List.copyOf(ids);
    }
}
