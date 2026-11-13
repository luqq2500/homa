package reservation.domain;

import kernel.Money;
import kernel.Percentage;

/**
 * The clear upfront price. The hours fee is the hourly rate per helper × helpers × hours;
 * supervisors are not charged. The provider price adds the travel fee when it is charged
 * separately; Homa's service fee is on top, paid by the customer.
 */
public record PriceBreakdown(Money hoursFee, Money travelFee, Money providerPrice, Money serviceFee, Money total) {

    public static PriceBreakdown of(ProviderTerms terms, BookingDetails details, Percentage serviceFeeRate) {
        Money hoursFee = terms.hourlyRate().times((long) details.helpers() * details.hours());
        Money travelFee = terms.travelFeeSeparate() ? terms.travelFee() : Money.ZERO;
        Money providerPrice = hoursFee.plus(travelFee);
        Money serviceFee = serviceFeeRate.of(providerPrice);
        return new PriceBreakdown(hoursFee, travelFee, providerPrice, serviceFee, providerPrice.plus(serviceFee));
    }

    /** The customer no-show fee: the travel fee plus a share of the hours fee. It is the ceiling of every cancellation fee. */
    public Money customerNoShowFee(Percentage rate) {
        return travelFee.plus(rate.of(hoursFee));
    }

    /** A cancellation fee: a share of the hours fee, never higher than the customer no-show fee. */
    public Money cancellationFee(Percentage rate, Percentage customerNoShowFeeRate) {
        return rate.of(hoursFee).min(customerNoShowFee(customerNoShowFeeRate));
    }

    /** A provider fine: a share of the hours fee. */
    public Money fine(Percentage rate) {
        return rate.of(hoursFee);
    }
}
