package reservation.usecase.dto;

import java.util.List;

public record EndProviderNoShowsResponse(List<String> ids) {

    public EndProviderNoShowsResponse {
        ids = List.copyOf(ids);
    }
}
