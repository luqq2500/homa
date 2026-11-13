package reservation.usecase.dto;

import java.util.List;

public record CloseEnquiriesResponse(List<String> ids) {

    public CloseEnquiriesResponse {
        ids = List.copyOf(ids);
    }
}
