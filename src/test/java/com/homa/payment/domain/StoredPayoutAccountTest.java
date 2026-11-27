package com.homa.payment.domain;

import static com.homa.payment.domain.PaymentFixtures.ACCOUNT_CHECKED_AT;
import static com.homa.payment.domain.PaymentFixtures.ACCOUNT_SUBMITTED_AT;
import static com.homa.payment.domain.PaymentFixtures.KEMAS_HOME;
import static com.homa.payment.domain.PaymentFixtures.KEMAS_HOMES_ACCOUNT;
import static com.homa.payment.domain.PaymentFixtures.KEMAS_HOMES_BANK_ACCOUNT;
import static com.homa.payment.domain.PaymentFixtures.KEMAS_HOMES_SECOND_ACCOUNT;
import static com.homa.payment.domain.PaymentFixtures.KEMAS_HOMES_SECOND_BANK_ACCOUNT;
import static com.homa.payment.domain.PaymentFixtures.kemasHomesSubmittedAccount;
import static com.homa.payment.domain.PaymentFixtures.kemasHomesVerifiedAccount;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.payment.domain.exception.PayoutAccountAlreadyVerifiedException;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class StoredPayoutAccountTest {

    private static PayoutAccount restored(PayoutAccount stored) {
        return stored(stored.status(), stored.checkedAt().orElse(null));
    }

    private static PayoutAccount stored(PayoutAccountStatus status, Instant checkedAt) {
        return PayoutAccount.restore(KEMAS_HOME, KEMAS_HOMES_ACCOUNT, PayoutAccountHolder.BUSINESS,
                KEMAS_HOMES_BANK_ACCOUNT, ACCOUNT_SUBMITTED_AT, status, checkedAt);
    }

    @Test
    @Tag("PRV-2")
    @DisplayName("Persistence: Kemas Home's verified payout account comes back verified, receives payouts and can't be resubmitted.")
    void aStoredVerifiedAccountReceivesPayouts() {
        PayoutAccount verified = kemasHomesVerifiedAccount();

        PayoutAccount stored = restored(verified);

        assertThat(stored).usingRecursiveComparison().isEqualTo(verified);
        assertThat(stored.verifiedBankAccount()).isEqualTo(KEMAS_HOMES_BANK_ACCOUNT);
        assertThatThrownBy(() -> stored.resubmit(KEMAS_HOMES_SECOND_ACCOUNT, PayoutAccountHolder.BUSINESS,
                KEMAS_HOMES_SECOND_BANK_ACCOUNT, ACCOUNT_CHECKED_AT))
                .isInstanceOf(PayoutAccountAlreadyVerifiedException.class);
    }

    @Test
    @Tag("PRV-2")
    @DisplayName("Persistence: a stored account waiting for its holder check, or one that failed it, comes back as it was and can still be decided or replaced.")
    void aStoredPendingOrUnverifiedAccountComesBackAsItWas() {
        PayoutAccount pending = kemasHomesSubmittedAccount();
        PayoutAccount notVerified = kemasHomesSubmittedAccount();
        notVerified.recordHolderCheck(KEMAS_HOMES_ACCOUNT, false, ACCOUNT_CHECKED_AT);

        PayoutAccount storedPending = restored(pending);
        PayoutAccount storedNotVerified = restored(notVerified);

        assertThat(storedPending).usingRecursiveComparison().isEqualTo(pending);
        assertThat(storedNotVerified).usingRecursiveComparison().isEqualTo(notVerified);
        assertThat(storedPending.recordHolderCheck(KEMAS_HOMES_ACCOUNT, true, ACCOUNT_CHECKED_AT)).isTrue();
        assertThat(storedNotVerified.canReceivePayouts()).isFalse();
        storedNotVerified.resubmit(KEMAS_HOMES_SECOND_ACCOUNT, PayoutAccountHolder.BUSINESS,
                KEMAS_HOMES_SECOND_BANK_ACCOUNT, ACCOUNT_CHECKED_AT);
        assertThat(storedNotVerified.status()).isEqualTo(PayoutAccountStatus.PENDING);
    }

    @Test
    @Tag("PRV-2")
    @DisplayName("Persistence: a stored account has a check time exactly once its holder check came back, and always a status.")
    void aStoredCheckTimeMustFitTheStatus() {
        assertThatThrownBy(() -> stored(PayoutAccountStatus.PENDING, ACCOUNT_CHECKED_AT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("A stored payout account needs ");
        assertThatThrownBy(() -> stored(PayoutAccountStatus.VERIFIED, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> stored(PayoutAccountStatus.NOT_VERIFIED, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> stored(null, null)).isInstanceOf(NullPointerException.class);
    }
}
