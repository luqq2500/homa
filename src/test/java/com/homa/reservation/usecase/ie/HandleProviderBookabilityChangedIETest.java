package com.homa.reservation.usecase.ie;

import static com.homa.fixtures.HomaFixtures.AISYAH_CUSTOMER_ID;
import static com.homa.fixtures.HomaFixtures.KEMAS_HOME_PROVIDER_ID;
import static com.homa.reservation.usecase.ReservationTestBed.standardCheckout;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.ie.events.ProviderBookabilityChangedIE;
import com.homa.reservation.domain.BookingRefusal;
import com.homa.reservation.domain.Reservation;
import com.homa.reservation.domain.ReservationStatus;
import com.homa.reservation.domain.exception.BookingRefusedException;
import com.homa.reservation.domain.replica.ProviderReplica;
import com.homa.reservation.usecase.ReservationTestBed;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class HandleProviderBookabilityChangedIETest {

    private final ReservationTestBed bed = ReservationTestBed.withKemasHomeAndAisyah();

    private void startCheckout() {
        bed.startCheckout.start(standardCheckout(AISYAH_CUSTOMER_ID, KEMAS_HOME_PROVIDER_ID));
    }

    @Test
    @Tag("PRV-2")
    @DisplayName("Kemas Home has no verified payout account any more: no one can start paying it, and booked reservations stay.")
    void providerThatStopsBeingBookableTakesNoNewBookings() {
        Reservation booked = bed.requestStandard();

        bed.bookabilityChanged.handle(bed.bookability(false, "NO_VERIFIED_PAYOUT_ACCOUNT"));

        assertThatThrownBy(this::startCheckout).isInstanceOfSatisfying(BookingRefusedException.class,
                refused -> assertThat(refused.refusal()).isEqualTo(BookingRefusal.PROVIDER_NOT_BOOKABLE));
        assertThat(bed.reload(booked).status()).isEqualTo(ReservationStatus.REQUESTED);
    }

    @Test
    @Tag("PRV-2")
    @DisplayName("Kemas Home is bookable again once its payout account is verified.")
    void providerBecomesBookableAgain() {
        bed.bookabilityChanged.handle(bed.bookability(false, "NO_VERIFIED_PAYOUT_ACCOUNT"));

        bed.bookabilityChanged.handle(bed.bookability(true, null));

        startCheckout();
        assertThat(bed.checkouts.all()).hasSize(1);
    }

    @Test
    @Tag("PRV-2")
    @DisplayName("A change heard before anything else about the provider is kept.")
    void changeForAnUnheardProviderIsKept() {
        bed.bookabilityChanged.handle(new ProviderBookabilityChangedIE(bed.nextInboundId(), "provider-new", true,
                null, bed.time.now()));

        assertThat(bed.providers.findById("provider-new"))
                .contains(ProviderReplica.unheardOf("provider-new").withBookability(true));
    }

    @Test
    @Tag("PRV-2")
    @DisplayName("A redelivered change changes nothing, even after a newer one.")
    void redeliveredChangeIsIgnored() {
        ProviderBookabilityChangedIE stopped = bed.bookability(false, "SUSPENDED");
        bed.bookabilityChanged.handle(stopped);
        bed.bookabilityChanged.handle(bed.bookability(true, null));

        bed.bookabilityChanged.handle(stopped);

        assertThat(bed.providers.findById(KEMAS_HOME_PROVIDER_ID).orElseThrow().bookable()).isTrue();
        assertThat(HandleProviderBookabilityChangedIE.HANDLER).isEqualTo("reservation.ProviderBookabilityChanged");
        assertThat(bed.publisher.published()).isEmpty();
    }
}
