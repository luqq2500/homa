package reservation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static reservation.domain.ReservationBuilders.A_PHOTO;
import static reservation.domain.ReservationBuilders.aReservation;
import static reservation.domain.ReservationBuilders.friday;
import static reservation.domain.ReservationBuilders.saturday;
import static reservation.domain.ReservationBuilders.thursday;

import java.util.List;
import org.junit.jupiter.api.Test;
import reservation.domain.exception.AccessNotesTooLongException;
import reservation.domain.exception.CompletionAlreadySubmittedException;
import reservation.domain.exception.InvalidStatusTransitionException;

/** Feature: Access notes. */
class ReservationAccessNotesTest {

    @Test
    void aRequestStartsWithNoNotesAndTakesTheAccountsNotes() {
        Reservation reservation = aReservation().requested();
        assertThat(reservation.accessNotes()).isEqualTo(AccessNotes.NONE);

        reservation.changeAccessNotes(" Unit 12-3, register at the guardhouse ", thursday("09:00"));

        assertThat(reservation.accessNotes().text()).isEqualTo("Unit 12-3, register at the guardhouse");
    }

    @Test
    void aReservationsNotesChangeUntilTheJobIsCompleted() {
        Reservation reservation = aReservation().reservedWithTeamNamed();

        reservation.changeAccessNotes("Park at B2", friday("18:00"));

        assertThat(reservation.accessNotes().text()).isEqualTo("Park at B2");
    }

    @Test
    void notesStillChangeDuringAJobDispute() {
        Reservation reservation = aReservation().checkedInAt(saturday("10:00"));
        reservation.fileReport(Party.CUSTOMER, ReportType.DURING_JOB, "Only one helper came", List.of(), saturday("10:30"));

        reservation.changeAccessNotes("Gate code changed", saturday("10:40"));

        assertThat(reservation.accessNotes().text()).isEqualTo("Gate code changed");
    }

    @Test
    void afterCompletionTheNotesNoLongerChange() {
        Reservation reservation = aReservation().completedAt(saturday("12:45"));

        assertThatThrownBy(() -> reservation.changeAccessNotes("Park at B2", saturday("13:00")))
                .isInstanceOf(CompletionAlreadySubmittedException.class);
        assertThat(reservation.accessNotes()).isEqualTo(AccessNotes.NONE);
    }

    @Test
    void anEndedReservationsNotesNoLongerChange() {
        Reservation reservation = aReservation().reserved();
        reservation.cancelByCustomer(friday("09:00"));

        assertThatThrownBy(() -> reservation.changeAccessNotes("Park at B2", friday("10:00")))
                .isInstanceOf(InvalidStatusTransitionException.class);
        assertThatThrownBy(() -> aReservation().requested().changeAccessNotes("Park at B2", null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void notesAreAtMost500Characters() {
        Reservation reservation = aReservation().requested();

        reservation.changeAccessNotes("x".repeat(500), thursday("09:00"));
        assertThatThrownBy(() -> reservation.changeAccessNotes("x".repeat(501), thursday("09:00")))
                .isInstanceOf(AccessNotesTooLongException.class)
                .hasMessage("Access notes are at most 500 characters, were 501");
        reservation.changeAccessNotes(null, thursday("09:00"));
        assertThat(reservation.accessNotes()).isEqualTo(AccessNotes.NONE);
    }
}
