package reservation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static reservation.domain.ReservationBuilders.aReservation;
import static reservation.domain.ReservationBuilders.saturday;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Feature: Safety, damage or loss reports. */
class ReservationSafetyReportTest {

    @Test
    void theCustomerReportsASafetyProblem() {
        Reservation reservation = aReservation().checkedInAt(saturday("10:00"));

        reservation.fileReport(Party.CUSTOMER, ReportType.SAFETY_DAMAGE_OR_LOSS, "A vase was broken", List.of(), saturday("11:00"));

        assertThat(reservation.status()).isEqualTo(ReservationStatus.DISPUTED);
        assertThat(reservation.reports()).extracting(Report::type).containsExactly(ReportType.SAFETY_DAMAGE_OR_LOSS);
    }
}
