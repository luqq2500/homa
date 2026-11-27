package com.homa.payment.usecase;

import com.homa.ie.events.PayoutReleasedIE;
import com.homa.kernel.Money;
import com.homa.payment.domain.BankAccount;
import com.homa.payment.domain.Payout;
import com.homa.payment.domain.PayoutAccount;
import com.homa.payment.domain.PayoutRelease;
import com.homa.payment.usecase.dto.ReleaseDuePayoutsRequest;
import com.homa.payment.usecase.dto.ReleaseDuePayoutsResponse;
import com.homa.payment.usecase.port.in.DuePayoutReleaser;
import com.homa.payment.usecase.port.out.EventIds;
import com.homa.payment.usecase.port.out.PaymentIEPublisher;
import com.homa.payment.usecase.port.out.PayoutAccountRepository;
import com.homa.payment.usecase.port.out.PayoutGateway;
import com.homa.payment.usecase.port.out.PayoutRepository;
import com.homa.payment.usecase.port.out.TimeSource;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The scheduler's run: every payout the domain finds releasable now (PAY-2, PAY-6, RPT-6) is released, with the
 * platform fee in force when its reservation was Requested taken off and rounded once (PAY-3, PAY-8), and sent to the
 * provider's verified payout account. A payout whose provider has no verified account waits, still pending, and is
 * released on a later run once the account is verified (PRV-2, PRV-10). Nothing is sent when nothing is left after
 * the platform fee. Publishes {@link PayoutReleasedIE} for each release.
 */
public class ReleaseDuePayouts implements DuePayoutReleaser {

    private final PayoutRepository payouts;
    private final PayoutAccountRepository accounts;
    private final PayoutGateway gateway;
    private final PaymentIEPublisher publisher;
    private final TimeSource time;
    private final EventIds eventIds;

    public ReleaseDuePayouts(PayoutRepository payouts, PayoutAccountRepository accounts, PayoutGateway gateway,
            PaymentIEPublisher publisher, TimeSource time, EventIds eventIds) {
        this.payouts = payouts;
        this.accounts = accounts;
        this.gateway = gateway;
        this.publisher = publisher;
        this.time = time;
        this.eventIds = eventIds;
    }

    @Override
    public ReleaseDuePayoutsResponse releaseDue(ReleaseDuePayoutsRequest request) {
        Instant now = time.now();
        List<String> released = new ArrayList<>();
        List<String> waiting = new ArrayList<>();
        for (Payout payout : payouts.findPendingWithProtectionWindowEndedBy(now)) {
            if (payout.releasableAt(now)) {
                List<String> outcome = releaseToVerifiedAccount(payout, now) ? released : waiting;
                outcome.add(payout.id().value());
            }
        }
        return new ReleaseDuePayoutsResponse(released, waiting);
    }

    /** False, releasing nothing, while the provider has no verified payout account. */
    private boolean releaseToVerifiedAccount(Payout payout, Instant now) {
        Optional<PayoutAccount> account = accounts.findByProviderId(payout.providerId())
                .filter(PayoutAccount::canReceivePayouts);
        account.ifPresent(verified -> release(payout, verified, now));
        return account.isPresent();
    }

    private void release(Payout payout, PayoutAccount account, Instant now) {
        PayoutRelease release = payout.release(now);
        payouts.save(payout);
        send(release, account);
        publisher.publish(new PayoutReleasedIE(eventIds.next(), release.payoutId().value(),
                payout.reservationId().value(), release.providerId().value(), release.kind().name(), release.gross(),
                release.platformFee(), release.afterPlatformFee(), release.releasedAt()));
    }

    private void send(PayoutRelease release, PayoutAccount account) {
        Money net = release.afterPlatformFee();
        if (net.isPositive()) {
            BankAccount bankAccount = account.verifiedBankAccount();
            gateway.send(new PayoutGateway.Transfer(IdempotencyKeys.payout(release.payoutId()),
                    account.accountId().value(), bankAccount.bank(), bankAccount.accountNumber(),
                    bankAccount.holderName(), net));
        }
    }
}
