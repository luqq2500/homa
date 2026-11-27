package com.homa.payment.domain;

import com.homa.payment.domain.exception.PayoutAccountAlreadyVerifiedException;
import com.homa.payment.domain.exception.PayoutAccountNotVerifiedException;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Where a provider's payouts are sent: one per provider. The provider submits an account held in the owner's or the
 * business's name, and it is PENDING while Verification checks the holder through the payment provider, against the
 * owner's verified identity or the business registration; only "matched" or "not matched" comes back (IDN-3). A
 * matched account is VERIFIED, and only a verified account lets payouts be sent and the provider be booked (PRV-2).
 *
 * <p>Until the account is verified the provider can submit another one. It replaces the one before, and a check
 * result for the replaced account changes nothing. Changing a verified account is PRV-10 (payouts wait until the new
 * account is verified, and the owner is told of every change); it replaces the current account the same way.
 */
public final class PayoutAccount {

    private final ProviderId providerId;
    private PayoutAccountId accountId;
    private PayoutAccountHolder holder;
    private BankAccount bankAccount;
    private Instant submittedAt;
    private PayoutAccountStatus status;
    private Instant checkedAt;
    private String gatewayAccountRef;

    private PayoutAccount(ProviderId providerId) {
        this.providerId = Objects.requireNonNull(providerId, "providerId");
    }

    /** The provider submits its payout account: it waits, PENDING, for Verification's holder check (PRV-2). */
    public static PayoutAccount submit(ProviderId providerId, PayoutAccountId accountId, PayoutAccountHolder holder,
            BankAccount bankAccount, Instant submittedAt) {
        PayoutAccount account = new PayoutAccount(providerId);
        account.replaceWith(accountId, holder, bankAccount, submittedAt);
        return account;
    }

    /**
     * The provider submits another account before the current one is verified, for instance after a failed check. It
     * replaces the current one and waits for its own check. A verified account is changed under PRV-10 instead.
     */
    public void resubmit(PayoutAccountId newAccountId, PayoutAccountHolder newHolder, BankAccount newBankAccount,
            Instant resubmittedAt) {
        if (status == PayoutAccountStatus.VERIFIED) {
            throw new PayoutAccountAlreadyVerifiedException(providerId, accountId);
        }
        replaceWith(newAccountId, newHolder, newBankAccount, resubmittedAt);
    }

    private void replaceWith(PayoutAccountId newAccountId, PayoutAccountHolder newHolder, BankAccount newBankAccount,
            Instant at) {
        this.accountId = Objects.requireNonNull(newAccountId, "accountId");
        this.holder = Objects.requireNonNull(newHolder, "holder");
        this.bankAccount = Objects.requireNonNull(newBankAccount, "bankAccount");
        this.submittedAt = Objects.requireNonNull(at, "submittedAt");
        this.status = PayoutAccountStatus.PENDING;
        this.checkedAt = null;
        this.gatewayAccountRef = null;
    }

    /**
     * The payment provider registered the current account and knows it by {@code reference}. Verification's holder
     * check names the account by this reference, so the bank account number never leaves Payment (IDN-3). An account
     * submitted to replace this one is registered afresh.
     */
    public void recordGatewayRegistration(String reference) {
        Ids.requireText(reference, "The payment provider's account reference");
        this.gatewayAccountRef = reference;
    }

    /**
     * Verification's holder check came back for {@code checkedAccount}: matched makes it VERIFIED, not matched makes it
     * NOT_VERIFIED. Only the current account, while it waits for its check, is decided this way; a result for an
     * account the provider has replaced, or a second result, changes nothing.
     *
     * @return true when this result decided the account; false when it changed nothing
     */
    public boolean recordHolderCheck(PayoutAccountId checkedAccount, boolean matched, Instant checkedAt) {
        Objects.requireNonNull(checkedAccount, "checkedAccount");
        Objects.requireNonNull(checkedAt, "checkedAt");
        if (!checkedAccount.equals(accountId) || status != PayoutAccountStatus.PENDING) {
            return false;
        }
        this.status = matched ? PayoutAccountStatus.VERIFIED : PayoutAccountStatus.NOT_VERIFIED;
        this.checkedAt = checkedAt;
        return true;
    }

    /** True only while the account is verified: payouts are sent to it, and wait while it isn't (PRV-2, PRV-10). */
    public boolean canReceivePayouts() {
        return status == PayoutAccountStatus.VERIFIED;
    }

    /** The bank details a payout is sent to. Only a verified account receives payouts (PRV-2, PRV-10). */
    public BankAccount verifiedBankAccount() {
        if (!canReceivePayouts()) {
            throw new PayoutAccountNotVerifiedException(providerId, accountId, status);
        }
        return bankAccount;
    }

    public ProviderId providerId() {
        return providerId;
    }

    /** The current account: the one submitted last. */
    public PayoutAccountId accountId() {
        return accountId;
    }

    public PayoutAccountHolder holder() {
        return holder;
    }

    public BankAccount bankAccount() {
        return bankAccount;
    }

    /** When the current account was submitted. */
    public Instant submittedAt() {
        return submittedAt;
    }

    public PayoutAccountStatus status() {
        return status;
    }

    /** When the current account's holder check came back, once it has. */
    public Optional<Instant> checkedAt() {
        return Optional.ofNullable(checkedAt);
    }

    /** How the payment provider knows the current account, once it is registered there. */
    public Optional<String> gatewayAccountRef() {
        return Optional.ofNullable(gatewayAccountRef);
    }
}
