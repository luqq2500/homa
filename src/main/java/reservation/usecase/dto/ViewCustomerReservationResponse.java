package reservation.usecase.dto;

import java.time.Instant;
import java.util.Set;
import kernel.Money;

public record ViewCustomerReservationResponse(String reservationId, String status, String ending, String customerName, String addressLine, String accessNotes, Set<String> jobIds, Instant start, Instant end, int helpers, boolean supervisorIncluded, Money providerPrice, Money serviceFee, Money total, boolean paymentSecured, Set<String> teamMemberIds, Instant checkedInAt, Instant disputeWindowEndsAt) {

    public ViewCustomerReservationResponse {
        jobIds = Set.copyOf(jobIds);
        teamMemberIds = Set.copyOf(teamMemberIds);
    }
}
