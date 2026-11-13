package reservation.usecase.dto;

import java.util.List;

public record CancelUndecidedFlaggedResponse(List<String> ids) {

    public CancelUndecidedFlaggedResponse {
        ids = List.copyOf(ids);
    }
}
