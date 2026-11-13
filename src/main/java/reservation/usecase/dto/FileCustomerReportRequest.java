package reservation.usecase.dto;

import java.util.List;

public record FileCustomerReportRequest(String customerId, String reservationId, String type, String reason, List<String> evidence) {

    public FileCustomerReportRequest {
        evidence = List.copyOf(evidence);
    }
}
