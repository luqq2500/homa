package com.homa.reservation.usecase;

import static com.homa.fixtures.HomaFixtures.AISYAH_CUSTOMER_ID;
import static com.homa.fixtures.HomaFixtures.JOB_END;
import static com.homa.fixtures.HomaFixtures.JOB_START;
import static com.homa.reservation.usecase.ReservationTestBed.KITCHEN_CLEANING;
import static com.homa.fixtures.HomaFixtures.KEMAS_HOME_PROVIDER_ID;
import static com.homa.fixtures.HomaFixtures.REQUESTED_AT;
import static com.homa.fixtures.HomaFixtures.at;
import static com.homa.reservation.usecase.ReservationTestBed.friday;
import static com.homa.reservation.usecase.ReservationTestBed.monday;
import static com.homa.reservation.usecase.ReservationTestBed.saturday;
import static com.homa.reservation.usecase.ReservationTestBed.sunday;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.ie.events.PaymentHeldIE;
import com.homa.ie.events.ReservationCancelledIE;
import com.homa.kernel.Money;
import com.homa.reservation.domain.Reservation;
import com.homa.reservation.domain.ReservationStatus;
import com.homa.reservation.domain.exception.JobAlreadyEndedException;
import com.homa.reservation.domain.exception.ReservationStatusConflictException;
import com.homa.reservation.usecase.dto.AcceptReservationRequest;
import com.homa.reservation.usecase.dto.CustomerCancelReservationRequest;
import com.homa.reservation.usecase.dto.ProviderCancelReservationRequest;
import com.homa.reservation.usecase.dto.ProviderCancelReservationResponse;
import com.homa.reservation.usecase.dto.StartReservationCheckoutRequest;
import com.homa.reservation.usecase.exception.NotReservationPartyException;
import com.homa.reservation.usecase.exception.ReservationNotFoundException;
import com.homa.reservation.usecase.exception.ScheduleChangedException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class ProviderCancelReservationTest {

    private final ReservationTestBed bed = ReservationTestBed.withKemasHomeAndAisyah();
    private final Reservation reserved = bed.reserveStandard();

    private ProviderCancelReservationResponse cancel(String actorId) {
        return bed.providerCancel.cancel(new ProviderCancelReservationRequest(reserved.id().value(), actorId));
    }

    private void assertStillReserved() {
        assertThat(bed.reload(reserved).status()).isEqualTo(ReservationStatus.RESERVED);
        assertThat(bed.schedules.providerBlocks(KEMAS_HOME_PROVIDER_ID)).hasSize(1);
        assertThat(bed.schedules.customerBlocks(AISYAH_CUSTOMER_ID)).hasSize(1);
        assertThat(bed.publisher.published()).isEmpty();
    }

    private ReservationCancelledIE cancelledEvent(String cause, Instant at, String refund, String compensation,
            String fine) {
        return new ReservationCancelledIE("reservation-event-4", reserved.id().value(), AISYAH_CUSTOMER_ID,
                KEMAS_HOME_PROVIDER_ID, cause, at, REQUESTED_AT, saturday(9, 45), sunday(14, 0),
                "settlement-" + reserved.id().value(), Money.myr(0), Money.myr(refund), Money.myr(compensation),
                Money.myr(fine), Money.myr(0), at);
    }

    @Test
    @Tag("PCX-1")
    @DisplayName("Kemas Home cancels on Wednesday: fined RM45, Aisyah gets RM200.")
    void providerIsFinedTheBaseCancellationFeeMoreThan24HoursAheadAndBothCalendarsOpen() {
        bed.publisher.clear();
        Instant wednesday = at(2026, 12, 2, 10, 0);
        bed.time.set(wednesday);

        ProviderCancelReservationResponse cancelled = cancel(KEMAS_HOME_PROVIDER_ID);

        assertThat(cancelled).isEqualTo(new ProviderCancelReservationResponse(reserved.id().value(), "CANCELLED",
                "PROVIDER_CANCELLED", wednesday, Money.myr(45), Money.myr(200), Money.myr(0)));
        Reservation saved = bed.reload(reserved);
        assertThat(saved.status()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(bed.schedules.providerBlocks(KEMAS_HOME_PROVIDER_ID)).isEmpty();
        assertThat(bed.schedules.customerBlocks(AISYAH_CUSTOMER_ID)).isEmpty();
        assertThat(bed.publisher.single(ReservationCancelledIE.class))
                .isEqualTo(cancelledEvent("PROVIDER_CANCELLED", wednesday, "200", "0", "45"));
    }

    @Test
    @Tag("PCX-2")
    @DisplayName("Kemas Home cancels on Friday 20:00: fined RM65, Aisyah gets RM206.")
    void providerIsFinedBasePlusTenPercentWithin24HoursAndTheCustomerIsCompensated() {
        bed.publisher.clear();
        bed.time.set(friday(20, 0));

        ProviderCancelReservationResponse cancelled = cancel(KEMAS_HOME_PROVIDER_ID);

        assertThat(cancelled).isEqualTo(new ProviderCancelReservationResponse(reserved.id().value(), "CANCELLED",
                "PROVIDER_CANCELLED", friday(20, 0), Money.myr(65), Money.myr(200), Money.myr(6)));
        assertThat(bed.publisher.single(ReservationCancelledIE.class))
                .isEqualTo(cancelledEvent("PROVIDER_CANCELLED", friday(20, 0), "200", "6", "65"));
    }

    @Test
    @Tag("LMC-3")
    @DisplayName("Kemas Home's van breaks down at 9:50: fined RM85, Aisyah gets RM214.")
    void providerCancellingInTheCheckInPeriodIsFinedBasePlusTwentyPercent() {
        bed.publisher.clear();
        bed.time.set(saturday(9, 50));

        ProviderCancelReservationResponse cancelled = cancel(KEMAS_HOME_PROVIDER_ID);

        assertThat(cancelled).isEqualTo(new ProviderCancelReservationResponse(reserved.id().value(), "CANCELLED",
                "PROVIDER_LAST_MINUTE", saturday(9, 50), Money.myr(85), Money.myr(200), Money.myr(14)));
        assertThat(bed.publisher.single(ReservationCancelledIE.class))
                .isEqualTo(cancelledEvent("PROVIDER_LAST_MINUTE", saturday(9, 50), "200", "14", "85"));
    }

    @Test
    @Tag("CAN-3")
    @DisplayName("Percentages in this section are of the Total Reservation Fee, unless stated otherwise. Base + 10% is 45 + 20 = RM65.")
    void percentagesAreOfTheTotalReservationFeeNotTheHoursFee() {
        bed.time.set(friday(20, 0));

        ProviderCancelReservationResponse cancelled = cancel(KEMAS_HOME_PROVIDER_ID);

        assertThat(cancelled.providerFine()).isEqualTo(Money.myr(65)).isNotEqualTo(Money.myr(63));
        assertThat(cancelled.compensation()).isEqualTo(Money.myr(6)).isNotEqualTo(Money.myr("5.40"));
    }

    @Test
    @Tag("CAN-2")
    @DisplayName("For a RM25 job, a fine of 25 + 20% × 25 = RM30 is capped at RM25.")
    void fineIsCappedAtTheTotalReservationFee() {
        ReservationTestBed cheap = ReservationTestBed.withKemasHomeAndAisyah();
        cheap.kemasRelists(listing -> {
            listing.rate = Money.myr(25);
            listing.travelFeeKind = "NONE";
            listing.flatTravelFee = null;
            listing.discountRate = null;
            listing.minHours = Duration.ofHours(1);
        });
        String checkoutId = cheap.startCheckout.start(new StartReservationCheckoutRequest(AISYAH_CUSTOMER_ID,
                KEMAS_HOME_PROVIDER_ID, List.of(KITCHEN_CLEANING), JOB_START, 60, 1)).checkoutId();
        cheap.time.set(REQUESTED_AT);
        cheap.paymentHeld.handle(new PaymentHeldIE(cheap.nextInboundId(), checkoutId, Money.myr(25), REQUESTED_AT,
                REQUESTED_AT));
        String id = cheap.reservations.single().id().value();
        cheap.time.set(monday(22, 0));
        cheap.acceptReservation.accept(new AcceptReservationRequest(id, KEMAS_HOME_PROVIDER_ID));
        cheap.publisher.clear();
        cheap.time.set(saturday(9, 50));

        ProviderCancelReservationResponse cancelled = cheap.providerCancel
                .cancel(new ProviderCancelReservationRequest(id, KEMAS_HOME_PROVIDER_ID));

        assertThat(cancelled.providerFine()).isEqualTo(Money.myr(25));
        assertThat(cancelled.customerRefund()).isEqualTo(Money.myr(25));
        assertThat(cancelled.compensation()).isEqualTo(Money.myr("1.75"));
        assertThat(cheap.publisher.single(ReservationCancelledIE.class).providerFine()).isEqualTo(Money.myr(25));
    }

    @Test
    @Tag("PCX-1")
    @DisplayName("Only Kemas Home cancels its reservation as the provider: neither Aisyah nor another provider can, and an unknown reservation is refused.")
    void onlyTheReservationsProviderCancels() {
        bed.publisher.clear();
        bed.time.set(friday(20, 0));

        assertThatThrownBy(() -> cancel(AISYAH_CUSTOMER_ID)).isInstanceOf(NotReservationPartyException.class);
        assertThatThrownBy(() -> cancel("provider-another")).isInstanceOf(NotReservationPartyException.class);
        assertThatThrownBy(() -> bed.providerCancel
                .cancel(new ProviderCancelReservationRequest("reservation-unknown", KEMAS_HOME_PROVIDER_ID)))
                .isInstanceOf(ReservationNotFoundException.class);

        assertStillReserved();
    }

    @Test
    @Tag("PCX-1")
    @DisplayName("A request not yet accepted is declined, not cancelled: Kemas Home can't cancel it, and nothing changes.")
    void aRequestedReservationCannotBeCancelledByTheProvider() {
        ReservationTestBed other = ReservationTestBed.withKemasHomeAndAisyah();
        Reservation requested = other.requestStandard();
        other.publisher.clear();
        other.time.set(monday(21, 0));

        assertThatThrownBy(() -> other.providerCancel
                .cancel(new ProviderCancelReservationRequest(requested.id().value(), KEMAS_HOME_PROVIDER_ID)))
                .isInstanceOf(ReservationStatusConflictException.class);

        assertThat(other.reload(requested).status()).isEqualTo(ReservationStatus.REQUESTED);
        assertThat(other.publisher.published()).isEmpty();
    }

    @Test
    @Tag("LMC-3")
    @DisplayName("The job ended at 14:00, so Kemas Home can no longer cancel it, and nothing changes.")
    void aJobThatEndedCannotBeCancelledByTheProvider() {
        bed.publisher.clear();
        bed.time.set(JOB_END);

        assertThatThrownBy(() -> cancel(KEMAS_HOME_PROVIDER_ID)).isInstanceOf(JobAlreadyEndedException.class);

        assertStillReserved();
    }

    @Test
    @Tag("CCX-6")
    @DisplayName("Aisyah's cancellation commits while Kemas Home's is being saved: Kemas Home's reloads, finds the reservation Cancelled and is refused, so no fine is published.")
    void providerCancellationThatLosesTheRaceToTheCustomersIsRefused() {
        bed.publisher.clear();
        bed.time.set(friday(22, 0));
        bed.reservations.beforeNextSave(() -> bed.customerCancel
                .cancel(new CustomerCancelReservationRequest(reserved.id().value(), AISYAH_CUSTOMER_ID)));

        assertThatThrownBy(() -> cancel(KEMAS_HOME_PROVIDER_ID)).isInstanceOf(ReservationStatusConflictException.class);

        assertThat(bed.reload(reserved).cancellation().orElseThrow().cause().name()).isEqualTo("CUSTOMER_CANCELLED");
        ReservationCancelledIE published = bed.publisher.single(ReservationCancelledIE.class);
        assertThat(published.cause()).isEqualTo("CUSTOMER_CANCELLED");
        assertThat(published.providerFine()).isEqualTo(Money.myr(0));
    }

    @Test
    @Tag("PCX-1")
    @DisplayName("When another save moved a schedule on meanwhile, the cancellation reloads and is saved on the next try; after three conflicts it fails and nothing changes.")
    void cancellationIsRetriedAfterAConflictAndGivesUpAfterThree() {
        bed.publisher.clear();
        bed.time.set(friday(20, 0));
        bed.reservations.conflictOnNextSaves(3);

        assertThatThrownBy(() -> cancel(KEMAS_HOME_PROVIDER_ID)).isInstanceOf(ScheduleChangedException.class);
        assertStillReserved();

        bed.reservations.conflictOnNextSaves(2);
        cancel(KEMAS_HOME_PROVIDER_ID);

        assertThat(bed.reload(reserved).status()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(bed.publisher.single(ReservationCancelledIE.class).providerFine()).isEqualTo(Money.myr(65));
    }
}
