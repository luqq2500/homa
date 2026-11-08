package reservation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static reservation.domain.ReservationBuilders.aReservation;
import static reservation.domain.ReservationBuilders.friday;
import static reservation.domain.ReservationBuilders.rm;
import static reservation.domain.ReservationBuilders.saturday;
import static reservation.domain.ReservationBuilders.thursday;

import java.time.Instant;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reservation.domain.exception.InvalidStatusTransitionException;

/** Feature: Provider cancellation and fines. Background: reserved for Saturday 10:00, hours fee RM180, total RM230. */
class ReservationProviderCancellationTest {

    static Stream<Arguments> cancellations() {
        return Stream.of(
                Arguments.of(friday("09:59"), "0.00"),
                Arguments.of(friday("10:00"), "36.00"),
                Arguments.of(saturday("09:59"), "36.00"));
    }

    @ParameterizedTest(name = "cancelling at {0}: fined {1}")
    @MethodSource("cancellations")
    void providerFinesDependOnTimingOnly(Instant at, String fine) {
        CancellationResult result = aReservation().reserved().cancelByProvider(at);

        assertThat(result.settlement().customerRefund()).isEqualTo(rm("230.00"));
        assertThat(result.settlement().providerEarning()).isEqualTo(rm("0.00"));
        assertThat(result.fineAmount()).isEqualTo(rm(fine));
    }

    @Test
    void cancellingBeforeTheFeeWindowCarriesNoFine() {
        Reservation reservation = aReservation().reserved();

        CancellationResult result = reservation.cancelByProvider(friday("09:59"));

        assertThat(result.fine()).isEmpty();
        assertThat(result.settlement().ending()).isEqualTo(Ending.PROVIDER_CANCELLED);
        assertThat(reservation.status()).isEqualTo(ReservationStatus.CANCELLED);
    }

    @Test
    void cancellingInsideTheFeeWindowCarriesTheLateCancellationFine() {
        Reservation reservation = aReservation().reserved();

        FineCharge fine = reservation.cancelByProvider(friday("10:00")).fine().orElseThrow();

        assertThat(fine.reason()).isEqualTo(FineReason.LATE_CANCELLATION);
        assertThat(fine.providerId()).isEqualTo("provider-1");
        assertThat(fine.reservationId()).isEqualTo(reservation.id());
    }

    @Test
    void cancellingAfterTheBookedStartButBeforeCheckInCarriesTheNoShowFine() {
        CancellationResult result = aReservation().reserved().cancelByProvider(saturday("10:05"));

        assertThat(result.settlement().customerRefund()).isEqualTo(rm("230.00"));
        assertThat(result.fineAmount()).isEqualTo(rm("54.00"));
        assertThat(result.fine().orElseThrow().reason()).isEqualTo(FineReason.NO_SHOW);
    }

    @Test
    void cancellingAtTheBookedStartCarriesTheNoShowFine() {
        assertThat(aReservation().reserved().cancelByProvider(saturday("10:00")).fineAmount()).isEqualTo(rm("54.00"));
    }

    @Test
    void cancellingAfterCheckInCarriesTheNoShowFine() {
        CancellationResult result = aReservation().checkedInAt(saturday("09:58")).cancelByProvider(saturday("10:30"));

        assertThat(result.settlement().customerRefund()).isEqualTo(rm("230.00"));
        assertThat(result.fineAmount()).isEqualTo(rm("54.00"));
    }

    @Test
    void cancellingAfterAnEarlyCheckInBeforeTheStartCarriesTheNoShowFine() {
        assertThat(aReservation().checkedInAt(saturday("09:50")).cancelByProvider(saturday("09:55")).fineAmount())
                .isEqualTo(rm("54.00"));
    }

    @Test
    void theFineDoesNotDependOnTheProvidersTier() {
        CancellationResult result = aReservation().onTier(CancellationTier.FLEXIBLE).reserved().cancelByProvider(saturday("09:00"));

        assertThat(result.fineAmount()).isEqualTo(rm("36.00"));
    }

    @Test
    void withTravelIncludedTheFineAppliesToTheFullProviderPrice() {
        Reservation reservation = aReservation().withTravelIncluded("35").reserved();
        assertThat(reservation.price().providerPrice()).isEqualTo(rm("210.00"));

        assertThat(reservation.cancelByProvider(saturday("09:00")).fineAmount()).isEqualTo(rm("42.00"));
    }

    @Test
    void aProviderDeclinesARequestRatherThanCancellingIt() {
        assertThatThrownBy(() -> aReservation().requested().cancelByProvider(thursday("12:00")))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }
}
