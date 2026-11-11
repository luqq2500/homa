package reservation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static reservation.domain.ReservationBuilders.aBooking;
import static reservation.domain.ReservationBuilders.anAddressIn;
import static reservation.domain.ReservationBuilders.jobs;
import static reservation.domain.ReservationBuilders.rm;
import static reservation.domain.ReservationBuilders.saturday;
import static reservation.domain.ReservationBuilders.theLaunchPolicy;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import kernel.Percentage;
import org.junit.jupiter.api.Test;
import reservation.domain.exception.NoJobsChosenException;
import reservation.domain.exception.ReservationDetailRequiredException;

/** The details, offering and policy a reservation is made of. */
class ReservationDetailsTest {

    @Test
    void aBookingHasAtLeastOneJob() {
        assertThatThrownBy(() -> new ReservationDetails(Set.of(), saturday("10:00"), 3, 2, anAddressIn("Cyberjaya")))
                .isInstanceOf(NoJobsChosenException.class);
    }

    @Test
    void aBookingEndsItsHoursAfterItStarts() {
        assertThat(aBooking().forHours(3).build().end()).isEqualTo(saturday("13:00"));
    }

    @Test
    void aBookingNeedsAStartAndAnAddress() {
        assertThatThrownBy(() -> new ReservationDetails(jobs("KITCHEN"), null, 3, 2, anAddressIn("Cyberjaya")))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ReservationDetails(jobs("KITCHEN"), saturday("10:00"), 3, 2, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void anAddressNeedsAStreetAndACity() {
        assertThat(Address.of(" 12 Jalan ", City.of(" Cyberjaya ")).street()).isEqualTo("12 Jalan");
        assertThat(City.of("Cyberjaya")).hasToString("Cyberjaya");
        assertThat(JobCode.of(" KITCHEN ")).hasToString("KITCHEN");
        assertThatThrownBy(() -> Address.of(" ", City.of("Cyberjaya"))).isInstanceOf(ReservationDetailRequiredException.class);
        assertThatThrownBy(() -> Address.of("12 Jalan", null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void anOfferingBelongsToAProvider() {
        assertThatThrownBy(() -> new OfferingSnapshot(
                        " ", rm("30"), Optional.empty(), 2, 4, 1, 3, jobs("KITCHEN"), Set.of(City.of("Cyberjaya")), CancellationTier.FIRM,
                        Optional.empty(), true))
                .isInstanceOf(ReservationDetailRequiredException.class);
    }

    @Test
    void aPolicyHasAFeeForEveryTier() {
        PolicySnapshot launch = theLaunchPolicy();

        assertThat(launch.cancellationFee(CancellationTier.FIRM)).isEqualTo(Percentage.of(20));
        assertThatThrownBy(() -> new PolicySnapshot(
                        1, launch.serviceFee(), launch.minimumNotice(), launch.bookingHorizon(), launch.requestExpiry(),
                        launch.feeWindow(), Map.of(CancellationTier.FLEXIBLE, Percentage.ZERO),
                        launch.providerLateCancellationFine(), launch.providerNoShowFine(), launch.earliestCheckIn(),
                        launch.checkInPeriod(), launch.checkInRadiusMetres(), launch.customerNoShowWait(),
                        launch.customerNoShowFee(), launch.noShowReportWindow(), launch.completionReminder(),
                        launch.completionDeadline(), launch.disputeWindowExtension(), launch.teamReminder()))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("STANDARD");
    }
}
