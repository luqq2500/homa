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

import com.homa.fixtures.HomaFixtures;
import com.homa.payment.domain.exception.PayoutAccountAlreadyVerifiedException;
import com.homa.payment.domain.exception.PayoutAccountNotVerifiedException;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class PayoutAccountTest {

    private static final Instant RESUBMITTED_AT = HomaFixtures.at(2026, 11, 24, 9, 0);
    private static final Instant SECOND_CHECK_AT = HomaFixtures.at(2026, 11, 24, 11, 0);

    @Test
    @Tag("PRV-2")
    @DisplayName("Azman submits Kemas Home Sdn Bhd's company account: it waits for the holder check, and no payout "
            + "can be sent to it yet.")
    void submittedAccountWaitsForTheHolderCheck() {
        PayoutAccount account = kemasHomesSubmittedAccount();

        assertThat(account.providerId()).isEqualTo(KEMAS_HOME);
        assertThat(account.accountId()).isEqualTo(KEMAS_HOMES_ACCOUNT);
        assertThat(account.holder()).isEqualTo(PayoutAccountHolder.BUSINESS);
        assertThat(account.bankAccount()).isEqualTo(KEMAS_HOMES_BANK_ACCOUNT);
        assertThat(account.submittedAt()).isEqualTo(ACCOUNT_SUBMITTED_AT);
        assertThat(account.status()).isEqualTo(PayoutAccountStatus.PENDING);
        assertThat(account.checkedAt()).isEmpty();
        assertThat(account.canReceivePayouts()).isFalse();
        assertThatThrownBy(account::verifiedBankAccount)
                .isInstanceOf(PayoutAccountNotVerifiedException.class)
                .hasMessageContaining(KEMAS_HOMES_ACCOUNT.value())
                .hasMessageContaining(KEMAS_HOME.value())
                .hasMessageContaining("PENDING");
    }

    @Test
    @Tag("PRV-2")
    @DisplayName("The holder matches Kemas Home Sdn Bhd: the account is verified and payouts are sent to it.")
    void matchedHolderVerifiesTheAccount() {
        PayoutAccount account = kemasHomesSubmittedAccount();

        boolean decided = account.recordHolderCheck(KEMAS_HOMES_ACCOUNT, true, ACCOUNT_CHECKED_AT);

        assertThat(decided).isTrue();
        assertThat(account.status()).isEqualTo(PayoutAccountStatus.VERIFIED);
        assertThat(account.checkedAt()).contains(ACCOUNT_CHECKED_AT);
        assertThat(account.canReceivePayouts()).isTrue();
        assertThat(account.verifiedBankAccount()).isEqualTo(KEMAS_HOMES_BANK_ACCOUNT);
    }

    @Test
    @Tag("PRV-2")
    @DisplayName("The holder doesn't match: the account is not verified, and no payout can be sent to it.")
    void unmatchedHolderLeavesTheAccountNotVerified() {
        PayoutAccount account = kemasHomesSubmittedAccount();

        boolean decided = account.recordHolderCheck(KEMAS_HOMES_ACCOUNT, false, ACCOUNT_CHECKED_AT);

        assertThat(decided).isTrue();
        assertThat(account.status()).isEqualTo(PayoutAccountStatus.NOT_VERIFIED);
        assertThat(account.checkedAt()).contains(ACCOUNT_CHECKED_AT);
        assertThat(account.canReceivePayouts()).isFalse();
        assertThatThrownBy(account::verifiedBankAccount)
                .isInstanceOf(PayoutAccountNotVerifiedException.class)
                .hasMessageContaining("NOT_VERIFIED");
    }

    @Test
    @Tag("PRV-2")
    @DisplayName("Verification reports the same check twice: the first result stands.")
    void secondResultForTheSameAccountChangesNothing() {
        PayoutAccount account = kemasHomesVerifiedAccount();

        boolean decided = account.recordHolderCheck(KEMAS_HOMES_ACCOUNT, false, SECOND_CHECK_AT);

        assertThat(decided).isFalse();
        assertThat(account.status()).isEqualTo(PayoutAccountStatus.VERIFIED);
        assertThat(account.checkedAt()).contains(ACCOUNT_CHECKED_AT);
    }

    @Test
    @Tag("PRV-2")
    @DisplayName("Kemas Home's Maybank account fails the check and Azman submits its CIMB account: it replaces the "
            + "first one and waits for its own check.")
    void accountSubmittedAfterAFailedCheckReplacesIt() {
        PayoutAccount account = kemasHomesSubmittedAccount();
        account.recordHolderCheck(KEMAS_HOMES_ACCOUNT, false, ACCOUNT_CHECKED_AT);

        account.resubmit(KEMAS_HOMES_SECOND_ACCOUNT, PayoutAccountHolder.OWNER, KEMAS_HOMES_SECOND_BANK_ACCOUNT,
                RESUBMITTED_AT);

        assertThat(account.providerId()).isEqualTo(KEMAS_HOME);
        assertThat(account.accountId()).isEqualTo(KEMAS_HOMES_SECOND_ACCOUNT);
        assertThat(account.holder()).isEqualTo(PayoutAccountHolder.OWNER);
        assertThat(account.bankAccount()).isEqualTo(KEMAS_HOMES_SECOND_BANK_ACCOUNT);
        assertThat(account.submittedAt()).isEqualTo(RESUBMITTED_AT);
        assertThat(account.status()).isEqualTo(PayoutAccountStatus.PENDING);
        assertThat(account.checkedAt()).isEmpty();
        assertThat(account.canReceivePayouts()).isFalse();
    }

    @Test
    @Tag("PRV-2")
    @Tag("IDN-3")
    @DisplayName("The payment provider registers Kemas Home's account under its own reference; the CIMB account that "
            + "replaces it has none until it is registered in turn.")
    void gatewayReferenceBelongsToTheCurrentAccount() {
        PayoutAccount account = kemasHomesSubmittedAccount();
        assertThat(account.gatewayAccountRef()).isEmpty();

        account.recordGatewayRegistration("gw-acct-maybank");
        assertThat(account.gatewayAccountRef()).contains("gw-acct-maybank");

        account.resubmit(KEMAS_HOMES_SECOND_ACCOUNT, PayoutAccountHolder.OWNER, KEMAS_HOMES_SECOND_BANK_ACCOUNT,
                RESUBMITTED_AT);
        assertThat(account.gatewayAccountRef()).isEmpty();

        account.recordGatewayRegistration("gw-acct-cimb");
        assertThat(account.gatewayAccountRef()).contains("gw-acct-cimb");
    }

    @Test
    @Tag("IDN-3")
    @DisplayName("A registration without the payment provider's reference is refused, and the account keeps none.")
    void gatewayReferenceIsNeverBlank() {
        PayoutAccount account = kemasHomesSubmittedAccount();

        assertThatThrownBy(() -> account.recordGatewayRegistration(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The payment provider's account reference must not be blank");
        assertThat(account.gatewayAccountRef()).isEmpty();
    }

    @Test
    @Tag("PRV-2")
    @DisplayName("Azman replaces the Maybank account before its check comes back: the late Maybank result changes "
            + "nothing, and the CIMB result decides.")
    void resultForAReplacedAccountChangesNothing() {
        PayoutAccount account = kemasHomesSubmittedAccount();
        account.resubmit(KEMAS_HOMES_SECOND_ACCOUNT, PayoutAccountHolder.BUSINESS, KEMAS_HOMES_SECOND_BANK_ACCOUNT,
                RESUBMITTED_AT);

        assertThat(account.recordHolderCheck(KEMAS_HOMES_ACCOUNT, true, ACCOUNT_CHECKED_AT)).isFalse();
        assertThat(account.status()).isEqualTo(PayoutAccountStatus.PENDING);
        assertThat(account.checkedAt()).isEmpty();

        assertThat(account.recordHolderCheck(KEMAS_HOMES_SECOND_ACCOUNT, true, SECOND_CHECK_AT)).isTrue();
        assertThat(account.verifiedBankAccount()).isEqualTo(KEMAS_HOMES_SECOND_BANK_ACCOUNT);
        assertThat(account.checkedAt()).contains(SECOND_CHECK_AT);
    }

    @Test
    @Tag("PRV-10")
    @DisplayName("Kemas Home's account is verified: another account is a payout account change, not a resubmission.")
    void verifiedAccountIsNotResubmitted() {
        PayoutAccount account = kemasHomesVerifiedAccount();

        assertThatThrownBy(() -> account.resubmit(KEMAS_HOMES_SECOND_ACCOUNT, PayoutAccountHolder.BUSINESS,
                KEMAS_HOMES_SECOND_BANK_ACCOUNT, RESUBMITTED_AT))
                .isInstanceOf(PayoutAccountAlreadyVerifiedException.class)
                .hasMessageContaining(KEMAS_HOME.value())
                .hasMessageContaining(KEMAS_HOMES_ACCOUNT.value());
        assertThat(account.accountId()).isEqualTo(KEMAS_HOMES_ACCOUNT);
        assertThat(account.canReceivePayouts()).isTrue();
    }

    @Test
    @DisplayName("Bank details are never blank, and the account number never shows in full.")
    void bankDetailsAreCompleteAndTheNumberIsMasked() {
        assertThatThrownBy(() -> new BankAccount(" ", "514011223344", "Kemas Home Sdn Bhd"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("bank");
        assertThatThrownBy(() -> new BankAccount("MAYBANK", "", "Kemas Home Sdn Bhd"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("account number");
        assertThatThrownBy(() -> new BankAccount("MAYBANK", "514011223344", null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("holder");

        assertThat(KEMAS_HOMES_BANK_ACCOUNT.toString())
                .isEqualTo("BankAccount[MAYBANK ****3344, Kemas Home Sdn Bhd]")
                .doesNotContain("514011223344");
        assertThat(new BankAccount("MAYBANK", "123", "Hana").toString()).isEqualTo("BankAccount[MAYBANK ****123, Hana]");
        assertThat(new BankAccount("MAYBANK", "1234", "Hana").toString())
                .isEqualTo("BankAccount[MAYBANK ****1234, Hana]");
    }
}
