package reservation.usecase.dto;

import java.time.Instant;
import kernel.Money;

public record ReportCustomerNoShowResponse(String reservationId, Money customerNoShowFee, Instant reportWindowClosesAt) {}
