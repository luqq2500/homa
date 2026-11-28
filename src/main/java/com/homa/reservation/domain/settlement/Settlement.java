package com.homa.reservation.domain.settlement;

import com.homa.kernel.Money;
import java.util.Objects;

/**
 * What a settled reservation moves, in the five lines Payment executes and never re-derives (plan, key decision 1).
 *
 * <ul>
 *   <li>{@code customerFee}: what the customer pays the provider (CCX-6, LMC-1); zero for a free outcome.</li>
 *   <li>{@code customerRefund}: what goes back to the customer from the held payment, paid at once (PAY-7).</li>
 *   <li>{@code compensation}: the extra amount Homa pays the customer on top of a full refund (PAY-4).</li>
 *   <li>{@code providerFine}: the fine added to the provider's account (PAY-4, PAY-5).</li>
 *   <li>{@code providerPayoutGross}: what the provider is owed from the reservation, a FEE payout held until the
 *       Protection Window ends (PAY-6).</li>
 * </ul>
 *
 * <p>{@link #of} is how a settlement is made. It applies CAN-2 (the fee and the fine are each at most the cap, the
 * Total Reservation Fee) and PAY-8: every line is worked out exactly by its policy and rounded once to the sen, half
 * up, and the refund is what was held minus the rounded fee, so refund + fee always equals the amount held. The
 * constructor itself only keeps the lines, for a settlement a store gives back.
 */
public record Settlement(
        Money customerFee,
        Money customerRefund,
        Money compensation,
        Money providerFine,
        Money providerPayoutGross) {

    public Settlement {
        Objects.requireNonNull(customerFee, "customerFee");
        Objects.requireNonNull(customerRefund, "customerRefund");
        Objects.requireNonNull(compensation, "compensation");
        Objects.requireNonNull(providerFine, "providerFine");
        Objects.requireNonNull(providerPayoutGross, "providerPayoutGross");
    }

    /**
     * CAN-2, PAY-8: the settlement of a payment of {@code held}, from the exact fee, compensation and fine a policy
     * worked out. The fee and the fine are capped at {@code cap}; each line is rounded once, and the refund is
     * {@code held} minus the rounded fee. The provider is owed the fee.
     */
    public static Settlement of(Money held, Money cap, Money exactCustomerFee, Money exactCompensation,
            Money exactProviderFine) {
        Money fee = exactCustomerFee.min(cap).roundedToSen();
        return new Settlement(fee, held.minus(fee), exactCompensation.roundedToSen(),
                exactProviderFine.min(cap).roundedToSen(), fee);
    }
}
