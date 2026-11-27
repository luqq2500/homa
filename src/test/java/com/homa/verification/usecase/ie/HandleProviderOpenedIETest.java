package com.homa.verification.usecase.ie;

import static com.homa.fixtures.HomaFixtures.KEMAS_HOME_PROVIDER_ID;
import static com.homa.fixtures.HomaFixtures.at;
import static org.assertj.core.api.Assertions.assertThat;

import com.homa.ie.events.ProviderOpenedIE;
import com.homa.verification.domain.PersonId;
import com.homa.verification.usecase.VerificationWorld;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class HandleProviderOpenedIETest {

    private static final ProviderOpenedIE KEMAS_HOME_OPENED = new ProviderOpenedIE("provider-event-1",
            KEMAS_HOME_PROVIDER_ID, "person-azman", at(2026, 10, 2, 9, 0));

    private final VerificationWorld world = new VerificationWorld();

    @Test
    @Tag("PRV-1")
    @DisplayName("Azman opened Kemas Home: Verification records him as its owner, to check its payout account against.")
    void theOpeningOwnerIsRecorded() {
        world.providerOpened.handle(KEMAS_HOME_OPENED);

        assertThat(world.providerOwners.ownerOf(KEMAS_HOME_PROVIDER_ID)).contains(new PersonId("person-azman"));
        assertThat(world.processedMessages.handled("provider-event-1", "verification.ProviderOpened")).isTrue();
        assertThat(world.publisher.published()).isEmpty();
    }

    @Test
    @Tag("PRV-1")
    @DisplayName("A redelivered ProviderOpenedIE changes nothing and publishes nothing.")
    void aRedeliveryChangesNothing() {
        world.providerOpened.handle(KEMAS_HOME_OPENED);
        world.providerOwners.ownedBy(KEMAS_HOME_PROVIDER_ID, "person-after");

        world.providerOpened.handle(KEMAS_HOME_OPENED);

        assertThat(world.providerOwners.ownerOf(KEMAS_HOME_PROVIDER_ID)).contains(new PersonId("person-after"));
        assertThat(world.publisher.published()).isEmpty();
        assertThat(HandleProviderOpenedIE.HANDLER).isEqualTo("verification.ProviderOpened");
    }
}
