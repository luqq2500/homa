package reservation.usecase.dto;

import java.util.List;

public record AcceptRequestResponse(String reservationId, List<String> declinedForClash) {

    public AcceptRequestResponse {
        declinedForClash = List.copyOf(declinedForClash);
    }
}
