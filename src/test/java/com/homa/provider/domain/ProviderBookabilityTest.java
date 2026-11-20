package com.homa.provider.domain;

import static com.homa.provider.domain.ProviderFixtures.liveKemasHome;
import static com.homa.provider.domain.ProviderFixtures.verifiedKemasHome;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class ProviderBookabilityTest {

    @Test
    @Tag("PRV-6")
    @DisplayName("Kemas Home is live with active helpers, so customers can book it.")
    void liveProviderWithAnActiveHelperIsBookable() {
        Provider kemasHome = liveKemasHome();

        assertThat(kemasHome.bookabilityFor(3)).isEqualTo(Bookability.BOOKABLE);
        assertThat(kemasHome.bookabilityFor(1)).isEqualTo(Bookability.BOOKABLE);
        assertThat(Bookability.BOOKABLE.isBookable()).isTrue();
    }

    @Test
    @Tag("PRV-6")
    @DisplayName("Siti, Nurul and Rina are all deactivated for a holiday week. Kemas Home disappears from search and is told why.")
    void providerWithNoActiveHelpersCannotBeBooked() {
        Bookability bookability = liveKemasHome().bookabilityFor(0);

        assertThat(bookability).isEqualTo(Bookability.NO_ACTIVE_HELPERS);
        assertThat(bookability.isBookable()).isFalse();
    }

    @Test
    @Tag("PRV-2")
    @DisplayName("A provider that hasn't gone live can't be booked, whatever its team.")
    void providerThatIsNotLiveCannotBeBooked() {
        Bookability bookability = verifiedKemasHome().bookabilityFor(3);

        assertThat(bookability).isEqualTo(Bookability.NOT_LIVE);
        assertThat(bookability.isBookable()).isFalse();
    }
}
