package com.homa.rating.domain;

import com.homa.kernel.Money;
import java.util.Optional;
import java.util.function.BiPredicate;

/**
 * Why a reservation became Cancelled, as Reservation names it on its Cancelled event, and what each cause adds to
 * reliability (RTG-10): a provider's cancellation, no roster and no show count against the provider; a customer's
 * cancellation counts against the customer only when the customer paid a fee, and a customer's no show always
 * counts. A free cancellation counts against no one, nor does a suspended customer, nor a provider suspended without
 * a fine (SUS-3, a monitoring flag, isn't a provider cancellation).
 */
public enum CancellationCause {
    /** REQ-2, CCX-1 to CCX-6: counts against the customer only when the customer paid a fee (CCX-6). */
    CUSTOMER_CANCELLED(PartyRole.CUSTOMER, IncidentKind.CANCELLATION, Proof.CUSTOMER_PAID_A_FEE),
    /** PCX-1, PCX-2. */
    PROVIDER_CANCELLED(PartyRole.PROVIDER, IncidentKind.CANCELLATION, Proof.ALWAYS),
    /** LMC-1, LMC-2: the customer pays a fee, so it counts. */
    CUSTOMER_LAST_MINUTE(PartyRole.CUSTOMER, IncidentKind.CANCELLATION, Proof.CUSTOMER_PAID_A_FEE),
    /** LMC-3, RPT-11. */
    PROVIDER_LAST_MINUTE(PartyRole.PROVIDER, IncidentKind.CANCELLATION, Proof.ALWAYS),
    /** NRS-1 to NRS-3. */
    NO_ROSTER(PartyRole.PROVIDER, IncidentKind.NO_ROSTER, Proof.ALWAYS),
    /** NSH-3, NSH-6. */
    PROVIDER_NO_SHOW(PartyRole.PROVIDER, IncidentKind.NO_SHOW, Proof.ALWAYS),
    /** NSH-5. */
    CUSTOMER_NO_SHOW(PartyRole.CUSTOMER, IncidentKind.NO_SHOW, Proof.ALWAYS),
    /** CUS-5: a suspended customer's reservations are cancelled free, and it isn't their cancellation. */
    CUSTOMER_SUSPENDED(PartyRole.CUSTOMER, IncidentKind.CANCELLATION, Proof.NEVER),
    /** SUS-1 to SUS-5: counts as a provider cancellation (SUS-1, SUS-4) only when the provider is fined. */
    PROVIDER_SUSPENDED(PartyRole.PROVIDER, IncidentKind.CANCELLATION, Proof.PROVIDER_WAS_FINED);

    /** What the settlement must show for the cause to count. */
    private enum Proof {
        ALWAYS((fee, fine) -> true),
        NEVER((fee, fine) -> false),
        CUSTOMER_PAID_A_FEE((fee, fine) -> fee.isPositive()),
        PROVIDER_WAS_FINED((fee, fine) -> fine.isPositive());

        private final BiPredicate<Money, Money> holds;

        Proof(BiPredicate<Money, Money> holds) {
            this.holds = holds;
        }
    }

    private final PartyRole side;
    private final IncidentKind kind;
    private final Proof proof;

    CancellationCause(PartyRole side, IncidentKind kind, Proof proof) {
        this.side = side;
        this.kind = kind;
        this.proof = proof;
    }

    /** The cause the event names. A name that isn't one of these is a broken contract, not a business case. */
    public static CancellationCause named(String name) {
        return valueOf(name);
    }

    /**
     * What this cause adds to the reliability of a party on {@code role}'s side, given what the customer paid and
     * what the provider was fined (RTG-10).
     */
    Optional<IncidentKind> countsAgainst(PartyRole role, Money customerFee, Money providerFine) {
        boolean counts = side == role && proof.holds.test(customerFee, providerFine);
        return counts ? Optional.of(kind) : Optional.empty();
    }
}
