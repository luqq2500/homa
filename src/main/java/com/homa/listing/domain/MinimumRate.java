package com.homa.listing.domain;

import com.homa.kernel.Money;
import java.util.Objects;

/**
 * Homa's minimum rate (OFR-11): a per-helper hourly rate after the provider's discount can't be below RM20. Kemas
 * Home charges RM25, so its largest possible discount is 20%; a provider charging RM40 could offer up to 50%.
 */
public record MinimumRate(Money perHelperHour) {

    /** RM20 per helper hour, after discount (§11). */
    public static final MinimumRate HOMA = new MinimumRate(ListingValues.MINIMUM_RATE);

    public MinimumRate {
        Objects.requireNonNull(perHelperHour, "perHelperHour");
    }

    /** True when the rate, after the discount's rate, is at least this minimum: RM25 with 20% off is RM20, allowed. */
    public boolean allows(HourlyRate rate, DiscountRule discount) {
        return !rate.afterDiscount(discount.rate()).isLessThan(perHelperHour);
    }
}
