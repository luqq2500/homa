package com.homa.reservation.usecase.dto;

import com.homa.kernel.Money;
import java.math.BigDecimal;

/** RES-6: the Hours Fee, travel fee and Total Reservation Fee, the Base Cancellation Fee, and the discount that applied.
 *
 * @param discountPercent the discount rate in percent, 0 when none applied (OFR-4) */
public record QuoteReservationResponse(Money hoursFee, Money travelFee, Money total, Money baseCancellationFee, BigDecimal discountPercent) {
}
