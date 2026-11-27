package com.homa.payment.usecase;

import com.homa.ie.events.PayoutAccountSubmittedIE;
import com.homa.payment.domain.BankAccount;
import com.homa.payment.domain.PayoutAccount;
import com.homa.payment.domain.PayoutAccountHolder;
import com.homa.payment.domain.PayoutAccountId;
import com.homa.payment.domain.ProviderId;
import com.homa.payment.usecase.dto.SubmitPayoutAccountRequest;
import com.homa.payment.usecase.dto.SubmitPayoutAccountResponse;
import com.homa.payment.usecase.port.in.PayoutAccountSubmitter;
import com.homa.payment.usecase.port.out.EventIds;
import com.homa.payment.usecase.port.out.PaymentIEPublisher;
import com.homa.payment.usecase.port.out.PayoutAccountRepository;
import com.homa.payment.usecase.port.out.TimeSource;
import java.time.Instant;

/**
 * A provider submits its payout account (PRV-2). A first account is submitted; while it isn't verified, another one
 * replaces it. Publishes {@link PayoutAccountSubmittedIE}, so Verification checks the holder. Changing a verified
 * account is {@code OwnerChangePayoutAccount} (PRV-10, slice 6); the domain refuses it here.
 */
public class ProviderSubmitPayoutAccount implements PayoutAccountSubmitter {

    private final PayoutAccountRepository accounts;
    private final PaymentIEPublisher publisher;
    private final TimeSource time;
    private final EventIds eventIds;

    public ProviderSubmitPayoutAccount(PayoutAccountRepository accounts, PaymentIEPublisher publisher,
            TimeSource time, EventIds eventIds) {
        this.accounts = accounts;
        this.publisher = publisher;
        this.time = time;
        this.eventIds = eventIds;
    }

    @Override
    public SubmitPayoutAccountResponse submit(SubmitPayoutAccountRequest request) {
        ProviderId providerId = new ProviderId(request.providerId());
        PayoutAccountId accountId = PayoutAccountId.generate();
        PayoutAccountHolder holder = PayoutAccountHolder.valueOf(request.holder());
        BankAccount bankAccount = new BankAccount(request.bank(), request.accountNumber(),
                request.accountHolderName());
        Instant now = time.now();
        PayoutAccount account = accounts.findByProviderId(providerId)
                .map(existing -> {
                    existing.resubmit(accountId, holder, bankAccount, now);
                    return existing;
                })
                .orElseGet(() -> PayoutAccount.submit(providerId, accountId, holder, bankAccount, now));
        accounts.save(account);
        publisher.publish(new PayoutAccountSubmittedIE(eventIds.next(), account.providerId().value(),
                account.accountId().value(), account.holder().name(), account.submittedAt()));
        return new SubmitPayoutAccountResponse(account.accountId().value(), account.status().name());
    }
}
