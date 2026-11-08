package reservation.domain;

import kernel.Money;
import kernel.Percentage;

/**
 * The price of a booking. The hours fee is the rate per helper times the helpers times the hours. The
 * provider price adds the travel fee when it is charged separately. Homa's service fee is a share of the
 * provider price, and the customer pays the total.
 */
public record PriceBreakdown(Money hoursFee, Money travelFee, Money providerPrice, Money serviceFee, Money total) {

    static PriceBreakdown of(OfferingSnapshot offering, int helpers, int hours, Percentage serviceFeeShare) {
        Money hoursFee = offering.hourlyRatePerHelper().times(helpers).times(hours);
        Money travelFee = offering.separateTravelFee().orElse(Money.ZERO);
        Money providerPrice = hoursFee.plus(travelFee);
        Money serviceFee = serviceFeeShare.of(providerPrice);
        return new PriceBreakdown(hoursFee, travelFee, providerPrice, serviceFee, providerPrice.plus(serviceFee));
    }
}
