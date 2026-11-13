package reservation.usecase.dto;

import java.util.List;

public record FileProviderReportRequest(String providerId, String reservationId, String type, String reason, List<String> evidence) {

    public FileProviderReportRequest {
        evidence = List.copyOf(evidence);
    }
}
