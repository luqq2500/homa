package reservation.domain;

import java.time.Instant;
import java.util.Map;

/** Everything a reservation holds, as plain data: what is stored and viewed. Parts that have not happened yet are null. */
public record ReservationState(
        String id,
        String customerId,
        String customerName,
        Address address,
        String accessNotes,
        ProviderTerms terms,
        HomaValues values,
        BookingDetails details,
        PriceBreakdown price,
        ReservationStatus status,
        Ending ending,
        Instant requestedAt,
        boolean paymentSecured,
        Instant acceptedAt,
        JobTeam jobTeam,
        boolean teamReminded,
        Instant checkedInAt,
        Map<String, FaceCheck> faceChecks,
        Completion completion,
        boolean completionReminded,
        Report customerReport,
        Report providerReport,
        DisputeCause disputeCause,
        DisputeCause disputeCauseBeforeFlag,
        Instant customerNoShowReportedAt,
        boolean customerNoShowFeeReleased,
        Settlement settlement,
        Fine fine) {

    public ReservationState {
        faceChecks = Map.copyOf(faceChecks);
    }
}
