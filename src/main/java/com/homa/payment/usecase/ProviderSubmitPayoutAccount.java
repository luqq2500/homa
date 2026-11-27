package com.homa.payment.usecase;

import com.homa.ie.events.PayoutAccountSubmittedIE;
import com.homa.payment.domain.BankAccount;
import com.homa.payment.domain.PayoutAccount;
import com.homa.payment.domain.PayoutAccountHolder;
import com.homa.payment.domain.PayoutAccountId;
import com.homa.payment.domain.ProviderId;
import com.homa.payment.usecase.dto.SubmitPayoutAccountRequest;
import com.homa.payment.usecase.dto.SubmitPayoutAccountResponse;
import com.homa.payment.usecase.exception.NotProviderOwnerException;
import com.homa.payment.usecase.exception.ProviderOwnerNotYetKnownException;
import com.homa.payment.usecase.port.in.PayoutAccountSubmitter;
import com.homa.payment.usecase.port.out.EventIds;
import com.homa.payment.usecase.port.out.PaymentIEPublisher;
import com.homa.payment.usecase.port.out.PayoutAccountRepository;
import com.homa.payment.usecase.port.out.PayoutGateway;
import com.homa.payment.usecase.port.out.ProviderOwners;
import com.homa.payment.usecase.port.out.TimeSource;
import java.time.Instant;

/**
 * A provider's owner submits its payout account (PRV-1, PRV-2). Only the owner {@code ProviderOpenedIE} named may;
 * until that event has reached Payment, nobody may. A first account is submitted; while it isn't verified, another
 * one replaces it. The account is registered with the payment provider, whose reference it keeps; publishes
 * {@link PayoutAccountSubmittedIE} with that reference, so Verification checks the holder without the bank account
 * number ever leaving Payment (IDN-3). Changing a verified account is {@code OwnerChangePayoutAccount} (PRV-10,
 * slice 6); the domain refuses it here, before anything is registered.
 */
public class ProviderSubmitPayoutAccount implements PayoutAccountSubmitter {

    private final ProviderOwners owners;
    private final PayoutAccountRepository accounts;
    private final PayoutGateway gateway;
    private final PaymentIEPublisher publisher;
    private final TimeSource time;
    private final EventIds eventIds;

    public ProviderSubmitPayoutAccount(ProviderOwners owners, PayoutAccountRepository accounts,
            PayoutGateway gateway, PaymentIEPublisher publisher, TimeSource time, EventIds eventIds) {
        this.owners = owners;
        this.accounts = accounts;
        this.gateway = gateway;
        this.publisher = publisher;
        this.time = time;
        this.eventIds = eventIds;
    }

    @Override
    public SubmitPayoutAccountResponse submit(SubmitPayoutAccountRequest request) {
        ProviderId providerId = new ProviderId(request.providerId());
        requireOwner(providerId, request.actingPersonId());
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
        account.recordGatewayRegistration(gateway.registerAccount(new PayoutGateway.AccountRegistration(
                accountId.value(), bankAccount.bank(), bankAccount.accountNumber(), bankAccount.holderName())));
        accounts.save(account);
        publisher.publish(new PayoutAccountSubmittedIE(eventIds.next(), account.providerId().value(),
                account.accountId().value(), account.gatewayAccountRef().orElseThrow(), account.holder().name(),
                account.submittedAt()));
        return new SubmitPayoutAccountResponse(account.accountId().value(), account.status().name());
    }

    private void requireOwner(ProviderId providerId, String personId) {
        String owner = owners.ownerOf(providerId)
                .orElseThrow(() -> new ProviderOwnerNotYetKnownException(providerId.value()));
        if (!owner.equals(personId)) {
            throw new NotProviderOwnerException(personId, providerId.value());
        }
    }
}
