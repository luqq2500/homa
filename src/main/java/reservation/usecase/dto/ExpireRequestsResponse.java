package reservation.usecase.dto;

import java.util.List;

public record ExpireRequestsResponse(List<String> ids) {

    public ExpireRequestsResponse {
        ids = List.copyOf(ids);
    }
}
