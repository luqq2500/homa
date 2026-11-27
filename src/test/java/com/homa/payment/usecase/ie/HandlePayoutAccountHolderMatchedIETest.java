package com.homa.payment.usecase.ie;

import static com.homa.fixtures.HomaFixtures.KEMAS_HOME_PROVIDER_ID;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.ACCOUNT_CHECKED_AT;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.KEMAS_HOMES_ACCOUNT;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.holderMatched;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.kemasHomesSubmittedAccount;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.ie.events.PayoutAccountVerifiedIE;
import com.homa.payment.domain.PayoutAccount;
import com.homa.payment.domain.PayoutAccountId;
import com.homa.payment.domain.PayoutAccountStatus;
import com.homa.payment.usecase.FixedTimeSource;
import com.homa.payment.usecase.InMemoryPayoutAccountRepository;
import com.homa.payment.usecase.InMemoryProcessedMessageStore;
import com.homa.payment.usecase.RecordingPaymentIEPublisher;
import com.homa.payment.usecase.SequentialEventIds;
import com.homa.payment.usecase.exception.PayoutAccountNotFoundException;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class HandlePayoutAccountHolderMatchedIETest {

    private static final Instant HANDLED_AT = Instant.parse("2026-11-23T07:00:05Z");

    private final InMemoryPayoutAccountRepository accounts = new InMemoryPayoutAccountRepository();
    private final RecordingPaymentIEPublisher publisher = new RecordingPaymentIEPublisher();
    private final InMemoryProcessedMessageStore processed = new InMemoryProcessedMessageStore();
    private final HandlePayoutAccountHolderMatchedIE handler = new HandlePayoutAccountHolderMatchedIE(accounts,
            publisher, processed, new FixedTimeSource(HANDLED_AT), new SequentialEventIds());
    private final PayoutAccount account = kemasHomesSubmittedAccount();

    HandlePayoutAccountHolderMatchedIETest() {
        accounts.given(account);
    }

    @Test
    @Tag("PRV-2")
    @DisplayName("The holder matches Kemas Home Sdn Bhd: the account is verified and Provider is told.")
    void matchedHolderVerifiesTheAccount() {
        handler.handle(holderMatched("verification-event-1", KEMAS_HOMES_ACCOUNT, true));

        assertThat(account.status()).isEqualTo(PayoutAccountStatus.VERIFIED);
        assertThat(account.checkedAt()).contains(ACCOUNT_CHECKED_AT);
        assertThat(accounts.saves()).containsExactly(account);
        assertThat(publisher.published()).containsExactly(new PayoutAccountVerifiedIE("event-1",
                KEMAS_HOME_PROVIDER_ID, KEMAS_HOMES_ACCOUNT.value(), "BUSINESS", true, HANDLED_AT));
        assertThat(processed.recorded()).containsExactly("verification-event-1|payment.PayoutAccountHolderMatched");
    }

    @Test
    @Tag("PRV-2")
    @DisplayName("The holder doesn't match: the account is not verified, and Provider is told it still has none.")
    void unmatchedHolderLeavesTheAccountNotVerified() {
        handler.handle(holderMatched("verification-event-1", KEMAS_HOMES_ACCOUNT, false));

        assertThat(account.status()).isEqualTo(PayoutAccountStatus.NOT_VERIFIED);
        assertThat(accounts.saves()).containsExactly(account);
        assertThat(publisher.published()).containsExactly(new PayoutAccountVerifiedIE("event-1",
                KEMAS_HOME_PROVIDER_ID, KEMAS_HOMES_ACCOUNT.value(), "BUSINESS", false, HANDLED_AT));
    }

    @Test
    @Tag("PRV-2")
    @DisplayName("A redelivered holder check changes nothing and publishes nothing.")
    void redeliveredEventChangesNothing() {
        handler.handle(holderMatched("verification-event-1", KEMAS_HOMES_ACCOUNT, true));
        publisher.clear();

        handler.handle(holderMatched("verification-event-1", KEMAS_HOMES_ACCOUNT, false));

        assertThat(account.status()).isEqualTo(PayoutAccountStatus.VERIFIED);
        assertThat(accounts.saves()).hasSize(1);
        assertThat(publisher.published()).isEmpty();
    }

    @Test
    @Tag("PRV-2")
    @DisplayName("A result for an account Kemas Home has since replaced changes nothing and publishes nothing.")
    void resultForAReplacedAccountChangesNothing() {
        handler.handle(holderMatched("verification-event-1", new PayoutAccountId("payout-account-replaced"), true));

        assertThat(account.status()).isEqualTo(PayoutAccountStatus.PENDING);
        assertThat(accounts.saves()).isEmpty();
        assertThat(publisher.published()).isEmpty();
        assertThat(processed.recorded()).hasSize(1);
    }

    @Test
    @DisplayName("A result for a provider Payment never saw an account from is refused, so it is retried and "
            + "reported.")
    void resultForAnUnknownProviderIsRefused() {
        InMemoryPayoutAccountRepository empty = new InMemoryPayoutAccountRepository();
        HandlePayoutAccountHolderMatchedIE onEmpty = new HandlePayoutAccountHolderMatchedIE(empty, publisher,
                processed, new FixedTimeSource(HANDLED_AT), new SequentialEventIds());

        assertThatThrownBy(() -> onEmpty.handle(holderMatched("verification-event-1", KEMAS_HOMES_ACCOUNT, true)))
                .isInstanceOf(PayoutAccountNotFoundException.class)
                .hasMessageContaining(KEMAS_HOME_PROVIDER_ID);
        assertThat(publisher.published()).isEmpty();
    }
}
