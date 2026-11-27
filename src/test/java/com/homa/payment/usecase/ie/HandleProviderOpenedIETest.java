package com.homa.payment.usecase.ie;

import static com.homa.fixtures.HomaFixtures.KEMAS_HOME_PROVIDER_ID;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.ACCOUNT_SUBMITTED_AT;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.AZMAN;
import static com.homa.payment.usecase.PaymentUsecaseFixtures.KEMAS_HOME;
import static org.assertj.core.api.Assertions.assertThat;

import com.homa.ie.events.ProviderOpenedIE;
import com.homa.payment.usecase.InMemoryProcessedMessageStore;
import com.homa.payment.usecase.InMemoryProviderOwners;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class HandleProviderOpenedIETest {

    private final InMemoryProviderOwners owners = new InMemoryProviderOwners();
    private final InMemoryProcessedMessageStore processed = new InMemoryProcessedMessageStore();
    private final HandleProviderOpenedIE handler = new HandleProviderOpenedIE(processed, owners);

    private static ProviderOpenedIE kemasHomeOpened() {
        return new ProviderOpenedIE("provider-event-1", KEMAS_HOME_PROVIDER_ID, AZMAN, ACCOUNT_SUBMITTED_AT);
    }

    @Test
    @Tag("PRV-1")
    @DisplayName("Azman opened Kemas Home: Payment records him as its owner.")
    void theOpeningOwnerIsRecorded() {
        handler.handle(kemasHomeOpened());

        assertThat(owners.ownerOf(KEMAS_HOME)).contains(AZMAN);
        assertThat(processed.recorded()).containsExactly("provider-event-1|payment.ProviderOpened");
    }

    @Test
    @Tag("PRV-1")
    @DisplayName("A redelivered ProviderOpenedIE changes nothing.")
    void aRedeliveryChangesNothing() {
        handler.handle(kemasHomeOpened());
        owners.record(KEMAS_HOME, "person-after");

        handler.handle(kemasHomeOpened());

        assertThat(owners.ownerOf(KEMAS_HOME)).contains("person-after");
        assertThat(processed.recorded()).hasSize(1);
        assertThat(HandleProviderOpenedIE.HANDLER).isEqualTo("payment.ProviderOpened");
    }
}
