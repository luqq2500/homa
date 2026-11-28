package com.homa.rating.domain;

import static com.homa.fixtures.HomaFixtures.BASE_CANCELLATION_FEE;
import static com.homa.fixtures.HomaFixtures.at;
import static com.homa.rating.domain.RatingFixtures.RESERVATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.kernel.Money;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class CancellationTest {

    private static final Instant FRIDAY_22_00 = at(2026, 12, 4, 22, 0);
    private static final Money NOTHING = Money.myr(0);

    private static Cancellation cancelled(CancellationCause cause, Money customerFee, Money providerFine) {
        return new Cancellation(RESERVATION, cause, customerFee, providerFine, FRIDAY_22_00);
    }

    @Test
    @Tag("RTG-10")
    @DisplayName("Aisyah cancels on Friday 22:00 and pays the RM45 fee: it counts against her as a cancellation, "
            + "and not against Kemas Home.")
    void aFeePayingCustomerCancellationCountsAgainstTheCustomer() {
        Cancellation cancellation = cancelled(CancellationCause.CUSTOMER_CANCELLED, BASE_CANCELLATION_FEE, NOTHING);

        assertThat(cancellation.countsAgainst(PartyRole.CUSTOMER)).contains(IncidentKind.CANCELLATION);
        assertThat(cancellation.countsAgainst(PartyRole.PROVIDER)).isEmpty();
        assertThat(cancellation.voidsRatings()).isFalse();
    }

    @Test
    @Tag("RTG-10")
    @DisplayName("Aisyah's free cancellation isn't counted.")
    void aFreeCustomerCancellationCountsAgainstNoOne() {
        Cancellation cancellation = cancelled(CancellationCause.CUSTOMER_CANCELLED, NOTHING, NOTHING);

        assertThat(cancellation.countsAgainst(PartyRole.CUSTOMER)).isEmpty();
        assertThat(cancellation.countsAgainst(PartyRole.PROVIDER)).isEmpty();
    }

    @Test
    @Tag("RTG-10")
    @DisplayName("Aisyah cancels at 9:50 and pays RM65: a last-minute cancellation counts against her; one that "
            + "paid nothing doesn't.")
    void aLastMinuteCustomerCancellationCountsWhenItPaidAFee() {
        assertThat(cancelled(CancellationCause.CUSTOMER_LAST_MINUTE, Money.myr(65), NOTHING)
                .countsAgainst(PartyRole.CUSTOMER)).contains(IncidentKind.CANCELLATION);
        assertThat(cancelled(CancellationCause.CUSTOMER_LAST_MINUTE, Money.myr(65), NOTHING)
                .countsAgainst(PartyRole.PROVIDER)).isEmpty();
        assertThat(cancelled(CancellationCause.CUSTOMER_LAST_MINUTE, NOTHING, NOTHING)
                .countsAgainst(PartyRole.CUSTOMER)).isEmpty();
    }

    @Test
    @Tag("RTG-10")
    @DisplayName("Kemas Home cancels on Wednesday and is fined RM45: a cancellation against Kemas Home, even if no "
            + "fine were levied; Aisyah's record is untouched.")
    void aProviderCancellationAlwaysCountsAgainstTheProvider() {
        for (CancellationCause cause : new CancellationCause[] {CancellationCause.PROVIDER_CANCELLED,
                CancellationCause.PROVIDER_LAST_MINUTE}) {
            Cancellation cancellation = cancelled(cause, NOTHING, BASE_CANCELLATION_FEE);

            assertThat(cancellation.countsAgainst(PartyRole.PROVIDER)).contains(IncidentKind.CANCELLATION);
            assertThat(cancellation.countsAgainst(PartyRole.CUSTOMER)).isEmpty();
        }
        assertThat(cancelled(CancellationCause.PROVIDER_CANCELLED, NOTHING, NOTHING)
                .countsAgainst(PartyRole.PROVIDER)).contains(IncidentKind.CANCELLATION);
    }

    @Test
    @Tag("RTG-10")
    @DisplayName("Only 1 of 2 helpers is rostered at 9:45: the No Roster counts against Kemas Home.")
    void aNoRosterCountsAgainstTheProvider() {
        Cancellation cancellation = cancelled(CancellationCause.NO_ROSTER, NOTHING, Money.myr(69));

        assertThat(cancellation.countsAgainst(PartyRole.PROVIDER)).contains(IncidentKind.NO_ROSTER);
        assertThat(cancellation.countsAgainst(PartyRole.CUSTOMER)).isEmpty();
    }

    @Test
    @Tag("RTG-10")
    @DisplayName("A Provider No Show counts against Kemas Home, and a Customer No Show against Aisyah.")
    void aNoShowCountsAgainstWhoDidNotShow() {
        Cancellation providerNoShow = cancelled(CancellationCause.PROVIDER_NO_SHOW, NOTHING, Money.myr(105));
        Cancellation customerNoShow = cancelled(CancellationCause.CUSTOMER_NO_SHOW, Money.myr("105.50"), NOTHING);

        assertThat(providerNoShow.countsAgainst(PartyRole.PROVIDER)).contains(IncidentKind.NO_SHOW);
        assertThat(providerNoShow.countsAgainst(PartyRole.CUSTOMER)).isEmpty();
        assertThat(customerNoShow.countsAgainst(PartyRole.CUSTOMER)).contains(IncidentKind.NO_SHOW);
        assertThat(customerNoShow.countsAgainst(PartyRole.PROVIDER)).isEmpty();
        assertThat(cancelled(CancellationCause.CUSTOMER_NO_SHOW, NOTHING, NOTHING)
                .countsAgainst(PartyRole.CUSTOMER)).contains(IncidentKind.NO_SHOW);
    }

    @Test
    @Tag("RTG-10")
    @DisplayName("Daniel is suspended and his request is cancelled with a full refund: it isn't his cancellation, "
            + "so it counts against no one.")
    void aCustomerSuspensionCountsAgainstNoOne() {
        Cancellation cancellation = cancelled(CancellationCause.CUSTOMER_SUSPENDED, NOTHING, NOTHING);

        assertThat(cancellation.countsAgainst(PartyRole.CUSTOMER)).isEmpty();
        assertThat(cancellation.countsAgainst(PartyRole.PROVIDER)).isEmpty();
    }

    @Test
    @Tag("RTG-10")
    @DisplayName("Kemas Home is suspended by Support on Thursday and fined RM45 as a provider cancellation (SUS-4): "
            + "it counts. Suspended by a monitoring flag, with no fine (SUS-3), it doesn't.")
    void aProviderSuspensionCountsOnlyWhenTheProviderIsFined() {
        Cancellation fined = cancelled(CancellationCause.PROVIDER_SUSPENDED, NOTHING, BASE_CANCELLATION_FEE);
        Cancellation flagged = cancelled(CancellationCause.PROVIDER_SUSPENDED, NOTHING, NOTHING);

        assertThat(fined.countsAgainst(PartyRole.PROVIDER)).contains(IncidentKind.CANCELLATION);
        assertThat(fined.countsAgainst(PartyRole.CUSTOMER)).isEmpty();
        assertThat(flagged.countsAgainst(PartyRole.PROVIDER)).isEqualTo(Optional.empty());
        assertThat(flagged.countsAgainst(PartyRole.CUSTOMER)).isEmpty();
    }

    @Test
    @Tag("NSH-6")
    @DisplayName("Only a Provider No Show voids the ratings of a reservation.")
    void onlyAProviderNoShowVoidsRatings() {
        for (CancellationCause cause : CancellationCause.values()) {
            assertThat(cancelled(cause, NOTHING, NOTHING).voidsRatings())
                    .as(cause.name()).isEqualTo(cause == CancellationCause.PROVIDER_NO_SHOW);
        }
    }

    @Test
    @Tag("RTG-10")
    @DisplayName("A cancellation names the cause the Cancelled event carries, and a cause that isn't one is refused.")
    void aCauseIsNamedAsTheEventNamesIt() {
        assertThat(CancellationCause.named("NO_ROSTER")).isEqualTo(CancellationCause.NO_ROSTER);
        assertThatThrownBy(() -> CancellationCause.named("WITHDRAWN")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Cancellation(null, CancellationCause.NO_ROSTER, NOTHING, NOTHING, FRIDAY_22_00))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Cancellation(RESERVATION, null, NOTHING, NOTHING, FRIDAY_22_00))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Cancellation(RESERVATION, CancellationCause.NO_ROSTER, null, NOTHING, FRIDAY_22_00))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Cancellation(RESERVATION, CancellationCause.NO_ROSTER, NOTHING, null, FRIDAY_22_00))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Cancellation(RESERVATION, CancellationCause.NO_ROSTER, NOTHING, NOTHING, null))
                .isInstanceOf(NullPointerException.class);
    }
}
