package reservation.usecase.dto;

import java.util.List;

public record ReleaseCustomerNoShowFeesResponse(List<String> ids) {

    public ReleaseCustomerNoShowFeesResponse {
        ids = List.copyOf(ids);
    }
}
