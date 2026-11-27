package com.homa.provider.domain;

import static com.homa.provider.domain.ProviderFixtures.kemasHomeWithVerifiedPayoutAccount;
import static com.homa.provider.domain.ProviderFixtures.liveKemasHome;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class ProviderBookabilityTest {

    @Test
    @Tag("PRV-2")
    @DisplayName("Kemas Home is live, with its payout account verified and active helpers, so customers can book it.")
    void liveProviderWithAVerifiedPayoutAccountAndAnActiveHelperIsBookable() {
        Provider kemasHome = liveKemasHome();

        assertThat(kemasHome.bookabilityFor(3)).isEqualTo(Bookability.BOOKABLE);
        assertThat(kemasHome.bookabilityFor(1)).isEqualTo(Bookability.BOOKABLE);
        assertThat(Bookability.BOOKABLE.isBookable()).isTrue();
    }

    @Test
    @Tag("PRV-2")
    @DisplayName("When all its helpers are deactivated for a holiday week, Kemas Home disappears from search.")
    void providerWithNoActiveHelpersCannotBeBooked() {
        Bookability bookability = liveKemasHome().bookabilityFor(0);

        assertThat(bookability).isEqualTo(Bookability.NO_ACTIVE_HELPERS);
        assertThat(bookability.isBookable()).isFalse();
    }

    @Test
    @Tag("PRV-2")
    @DisplayName("Kemas Home's payout account is no longer verified, so it can't be booked until one is verified again.")
    void providerWithoutAVerifiedPayoutAccountCannotBeBooked() {
        Provider kemasHome = liveKemasHome();

        kemasHome.recordPayoutAccountNotVerified();

        assertThat(kemasHome.bookabilityFor(3)).isEqualTo(Bookability.NO_VERIFIED_PAYOUT_ACCOUNT);
        assertThat(Bookability.NO_VERIFIED_PAYOUT_ACCOUNT.isBookable()).isFalse();

        kemasHome.recordPayoutAccountVerified(PayoutAccountHolder.BUSINESS);

        assertThat(kemasHome.bookabilityFor(3)).isEqualTo(Bookability.BOOKABLE);
    }

    @Test
    @Tag("PRV-2")
    @DisplayName("With no verified payout account and no active helper, the payout account is named first: only the owner can fix it.")
    void missingPayoutAccountIsNamedBeforeMissingHelpers() {
        Provider kemasHome = liveKemasHome();

        kemasHome.recordPayoutAccountNotVerified();

        assertThat(kemasHome.bookabilityFor(0)).isEqualTo(Bookability.NO_VERIFIED_PAYOUT_ACCOUNT);
    }

    @Test
    @Tag("PRV-2")
    @DisplayName("A provider that hasn't gone live can't be booked, whatever its team and payout account.")
    void providerThatIsNotLiveCannotBeBooked() {
        Bookability bookability = kemasHomeWithVerifiedPayoutAccount().bookabilityFor(3);

        assertThat(bookability).isEqualTo(Bookability.NOT_LIVE);
        assertThat(bookability.isBookable()).isFalse();
    }

    @Test
    @Tag("PRV-2")
    @DisplayName("A verified payout account is held in someone's name: the owner's or the business's.")
    void verifiedPayoutAccountNamesItsHolder() {
        Provider kemasHome = liveKemasHome();

        assertThatThrownBy(() -> kemasHome.recordPayoutAccountVerified(null)).isInstanceOf(NullPointerException.class);
        assertThat(kemasHome.verifiedPayoutAccount()).contains(PayoutAccountHolder.BUSINESS);
    }
}
