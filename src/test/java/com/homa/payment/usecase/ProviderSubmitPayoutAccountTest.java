package com.homa.payment.usecase;

import static com.homa.fixtures.HomaFixtures.KEMAS_HOME_NAME;
import static com.homa.fixtures.HomaFixtures.KEMAS_HOME_PROVIDER_ID;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.ACCOUNT_CHECKED_AT;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.ACCOUNT_SUBMITTED_AT;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.AZMAN;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.KEMAS_HOME;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.KEMAS_HOMES_ACCOUNT;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.kemasHomesAccountRequest;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.kemasHomesSubmittedAccount;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.kemasHomesVerifiedAccount;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.ie.events.PayoutAccountSubmittedIE;
import com.homa.payment.domain.BankAccount;
import com.homa.payment.domain.PayoutAccount;
import com.homa.payment.domain.PayoutAccountHolder;
import com.homa.payment.domain.PayoutAccountStatus;
import com.homa.payment.domain.exception.PayoutAccountAlreadyVerifiedException;
import com.homa.payment.usecase.dto.SubmitPayoutAccountRequest;
import com.homa.payment.usecase.dto.SubmitPayoutAccountResponse;
import com.homa.payment.usecase.exception.NotProviderOwnerException;
import com.homa.payment.usecase.exception.ProviderOwnerNotYetKnownException;
import com.homa.payment.usecase.port.out.PayoutGateway;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class ProviderSubmitPayoutAccountTest {

    private final InMemoryProviderOwners owners = new InMemoryProviderOwners();
    private final InMemoryPayoutAccountRepository accounts = new InMemoryPayoutAccountRepository();
    private final RecordingPaymentIEPublisher publisher = new RecordingPaymentIEPublisher();
    private final RecordingPayoutGateway gateway = new RecordingPayoutGateway();
    private final FixedTimeSource time = new FixedTimeSource(ACCOUNT_SUBMITTED_AT);
    private final ProviderSubmitPayoutAccount submitter =
            new ProviderSubmitPayoutAccount(owners, accounts, gateway, publisher, time, new SequentialEventIds());

    @BeforeEach
    void azmanOpenedKemasHome() {
        owners.record(KEMAS_HOME, AZMAN);
    }

    @Test
    @Tag("PRV-2")
    @Tag("IDN-3")
    @DisplayName("Azman submits Kemas Home Sdn Bhd's company account: the payment provider registers it, and it waits "
            + "for Verification's holder check, which names it by the provider's reference, never its number.")
    void submittedAccountWaitsForTheHolderCheck() {
        SubmitPayoutAccountResponse response = submitter.submit(kemasHomesAccountRequest());

        PayoutAccount saved = accounts.findByProviderId(KEMAS_HOME).orElseThrow();
        assertThat(accounts.saves()).containsExactly(saved);
        assertThat(saved.accountId().value()).isEqualTo(response.payoutAccountId());
        assertThat(saved.holder()).isEqualTo(PayoutAccountHolder.BUSINESS);
        assertThat(saved.bankAccount()).isEqualTo(new BankAccount("MAYBANK", "514011223344", KEMAS_HOME_NAME));
        assertThat(saved.submittedAt()).isEqualTo(ACCOUNT_SUBMITTED_AT);
        assertThat(saved.status()).isEqualTo(PayoutAccountStatus.PENDING);
        assertThat(saved.gatewayAccountRef()).contains("gateway-account-1");
        assertThat(response.status()).isEqualTo("PENDING");
        assertThat(gateway.registrations()).containsExactly(new PayoutGateway.AccountRegistration(
                response.payoutAccountId(), "MAYBANK", "514011223344", KEMAS_HOME_NAME));
        assertThat(publisher.published()).containsExactly(new PayoutAccountSubmittedIE("event-1",
                KEMAS_HOME_PROVIDER_ID, response.payoutAccountId(), "gateway-account-1", "BUSINESS",
                ACCOUNT_SUBMITTED_AT));
        assertThat(publisher.published().getFirst().toString()).doesNotContain("514011223344");
    }

    @Test
    @Tag("PRV-2")
    @DisplayName("Kemas Home's first account failed the holder check: Azman's next account replaces it and is "
            + "checked in turn.")
    void accountSubmittedAfterAFailedCheckReplacesIt() {
        PayoutAccount failed = kemasHomesSubmittedAccount();
        failed.recordHolderCheck(KEMAS_HOMES_ACCOUNT, false, ACCOUNT_CHECKED_AT);
        accounts.given(failed);
        Instant nextDay = Instant.parse("2026-11-24T01:00:00Z");
        time.set(nextDay);

        SubmitPayoutAccountResponse response = submitter.submit(new SubmitPayoutAccountRequest(AZMAN,
                KEMAS_HOME_PROVIDER_ID, "OWNER", "CIMB", "8001234567", "Azman bin Ali"));

        assertThat(response.payoutAccountId()).isNotEqualTo(KEMAS_HOMES_ACCOUNT.value());
        assertThat(response.status()).isEqualTo("PENDING");
        assertThat(accounts.saves()).containsExactly(failed);
        assertThat(failed.accountId().value()).isEqualTo(response.payoutAccountId());
        assertThat(failed.bankAccount()).isEqualTo(new BankAccount("CIMB", "8001234567", "Azman bin Ali"));
        assertThat(failed.gatewayAccountRef()).contains("gateway-account-1");
        assertThat(gateway.registrations()).containsExactly(new PayoutGateway.AccountRegistration(
                response.payoutAccountId(), "CIMB", "8001234567", "Azman bin Ali"));
        assertThat(publisher.published()).containsExactly(new PayoutAccountSubmittedIE("event-1",
                KEMAS_HOME_PROVIDER_ID, response.payoutAccountId(), "gateway-account-1", "OWNER", nextDay));
    }

    @Test
    @Tag("PRV-10")
    @DisplayName("Kemas Home's account is verified: a new one is a payout account change, refused here.")
    void verifiedAccountIsNotReplacedBySubmittingAnother() {
        PayoutAccount verified = kemasHomesVerifiedAccount();
        accounts.given(verified);

        assertThatThrownBy(() -> submitter.submit(kemasHomesAccountRequest()))
                .isInstanceOf(PayoutAccountAlreadyVerifiedException.class);
        assertThat(verified.accountId()).isEqualTo(KEMAS_HOMES_ACCOUNT);
        assertThat(gateway.registrations()).isEmpty();
        assertThat(accounts.saves()).isEmpty();
        assertThat(publisher.published()).isEmpty();
    }

    @Test
    @Tag("PRV-2")
    @DisplayName("An account held by anyone but the owner or the business is refused.")
    void unknownHolderIsRefused() {
        assertThatThrownBy(() -> submitter.submit(new SubmitPayoutAccountRequest(AZMAN, KEMAS_HOME_PROVIDER_ID,
                "FRIEND", "MAYBANK", "514011223344", KEMAS_HOME_NAME)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(accounts.saves()).isEmpty();
        assertThat(publisher.published()).isEmpty();
    }

    @Test
    @Tag("PRV-1")
    @DisplayName("Hana isn't Kemas Home's owner, so she can't submit its payout account.")
    void onlyTheOwnerSubmitsThePayoutAccount() {
        assertThatThrownBy(() -> submitter.submit(new SubmitPayoutAccountRequest("person-hana",
                KEMAS_HOME_PROVIDER_ID, "OWNER", "MAYBANK", "514011223344", "Hana")))
                .isInstanceOf(NotProviderOwnerException.class)
                .hasMessage("person-hana isn't the owner of provider provider-kemas-home");
        assertThat(accounts.saves()).isEmpty();
        assertThat(publisher.published()).isEmpty();
    }

    @Test
    @Tag("PRV-1")
    @DisplayName("Before Payment has heard that Hana Cleaning opened, nobody can submit its payout account yet; Hana "
            + "can try again shortly.")
    void aPayoutAccountWaitsUntilTheOwnerIsKnown() {
        assertThatThrownBy(() -> submitter.submit(new SubmitPayoutAccountRequest("person-hana",
                "provider-hana-cleaning", "OWNER", "MAYBANK", "514011223344", "Hana")))
                .isInstanceOf(ProviderOwnerNotYetKnownException.class)
                .hasMessage("The owner of provider provider-hana-cleaning isn't known to Payment yet; try again "
                        + "shortly");
        assertThat(accounts.saves()).isEmpty();
        assertThat(publisher.published()).isEmpty();
    }

    @Test
    @DisplayName("A registration never writes the account number into a log.")
    void registrationHidesTheAccountNumber() {
        assertThat(new PayoutGateway.AccountRegistration("payout-account-1", "MAYBANK", "514011223344",
                KEMAS_HOME_NAME).toString()).isEqualTo("AccountRegistration[payout-account-1, MAYBANK]");
    }

    @Test
    @DisplayName("The request never writes the account number into a log.")
    void requestHidesTheAccountNumber() {
        assertThat(kemasHomesAccountRequest().toString())
                .isEqualTo("SubmitPayoutAccountRequest[person-azman, provider-kemas-home, BUSINESS, MAYBANK]");
    }
}
