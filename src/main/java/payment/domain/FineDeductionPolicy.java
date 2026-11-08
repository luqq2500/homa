package payment.domain;

import java.util.Comparator;
import java.util.List;
import kernel.Money;
import kernel.Percentage;
import payment.domain.exception.FineOfAnotherProviderException;

/**
 * Deducts a provider's outstanding fines from a payout for a completed job, oldest fine first, so the
 * provider always takes home part of every job: no more than the fine deduction cap of the payout goes to
 * fines. The cap rounds in the provider's favour.
 */
public final class FineDeductionPolicy {

    private FineDeductionPolicy() {
    }

    public static Payout apply(Payout payout, List<Fine> outstanding, Percentage cap) {
        outstanding.stream().filter(fine -> !fine.providerId().equals(payout.providerId())).findFirst().ifPresent(fine -> {
            throw new FineOfAnotherProviderException(fine.id(), payout.providerId());
        });
        payout.startDeducting();
        if (payout.kind() != PayoutKind.COMPLETED_JOB) {
            return payout;
        }
        Money budget = mostDeductible(payout.gross(), cap);
        List<Fine> oldestFirst = outstanding.stream().sorted(Comparator.comparing(Fine::chargedAt)).toList();
        for (Fine fine : oldestFirst) {
            budget = budget.minus(deductFrom(fine, budget, payout));
        }
        return payout;
    }

    /** The cap of the payout, rounded down to the sen, so the provider keeps at least the rest. */
    static Money mostDeductible(Money gross, Percentage cap) {
        Percentage kept = Percentage.ofBasisPoints(Percentage.of(100).basisPoints() - cap.basisPoints());
        return gross.minus(kept.of(gross));
    }

    private static Money deductFrom(Fine fine, Money budget, Payout payout) {
        Money part = fine.outstanding().min(budget);
        if (!part.isZero()) {
            fine.deduct(part);
            payout.deduct(fine.id(), part);
        }
        return part;
    }
}
