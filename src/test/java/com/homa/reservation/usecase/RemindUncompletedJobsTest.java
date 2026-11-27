package com.homa.reservation.usecase;

import static com.homa.fixtures.HomaFixtures.JOB_END;
import static com.homa.fixtures.HomaFixtures.KEMAS_HOME_PROVIDER_ID;
import static com.homa.reservation.usecase.ReservationTestBed.CLEAN_KITCHEN_PHOTO;
import static com.homa.reservation.usecase.ReservationTestBed.saturday;
import static org.assertj.core.api.Assertions.assertThat;

import com.homa.reservation.domain.Reservation;
import com.homa.reservation.usecase.InMemoryPorts.Reminder;
import com.homa.reservation.usecase.dto.CompleteReservationRequest;
import com.homa.reservation.usecase.dto.RemindUncompletedJobsRequest;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class RemindUncompletedJobsTest {

    private final ReservationTestBed bed = ReservationTestBed.withKemasHomeAndAisyah();

    private int remindAt(Instant now) {
        bed.time.set(now);
        return bed.remindUncompletedJobs.remind(new RemindUncompletedJobsRequest()).remindersSent();
    }

    @Test
    @Tag("JOB-5")
    @DisplayName("Kemas Home is reminded at 14:00 and 16:00, once each however often the scheduler runs.")
    void providerIsRemindedAtTheJobEndAndTwoHoursLater() {
        Reservation reserved = bed.reserveStandard();
        String id = reserved.id().value();

        assertThat(remindAt(saturday(13, 59))).isZero();
        assertThat(remindAt(saturday(14, 0))).isEqualTo(1);
        assertThat(remindAt(saturday(14, 5))).isZero();
        assertThat(remindAt(saturday(16, 0))).isEqualTo(1);
        assertThat(remindAt(saturday(16, 5))).isZero();

        assertThat(bed.notifier.sent()).containsExactly(
                new Reminder(id, KEMAS_HOME_PROVIDER_ID, JOB_END, saturday(14, 0)),
                new Reminder(id, KEMAS_HOME_PROVIDER_ID, JOB_END, saturday(16, 0)));
    }

    @Test
    @Tag("JOB-5")
    @DisplayName("Kemas Home marks the job at 15:00, so it isn't reminded at 16:00.")
    void markedJobIsNotRemindedAgain() {
        Reservation reserved = bed.reserveStandard();
        remindAt(saturday(14, 0));
        bed.photoStore.upload(CLEAN_KITCHEN_PHOTO);
        bed.time.set(saturday(15, 0));
        bed.completeReservation.complete(new CompleteReservationRequest(reserved.id().value(),
                KEMAS_HOME_PROVIDER_ID, List.of(CLEAN_KITCHEN_PHOTO)));

        assertThat(remindAt(saturday(16, 0))).isZero();
        assertThat(bed.notifier.sent()).hasSize(1);
    }

    @Test
    @Tag("JOB-5")
    @DisplayName("A scheduler that missed 14:00 sends both reminders at 16:00.")
    void missedReminderIsSentLate() {
        bed.reserveStandard();

        assertThat(remindAt(saturday(16, 0))).isEqualTo(2);
        assertThat(bed.notifier.sent()).extracting(Reminder::reminderAt)
                .containsExactly(saturday(14, 0), saturday(16, 0));
    }

    @Test
    @Tag("JOB-5")
    @DisplayName("A request never accepted is no job to mark Completed, so there is nothing to remind.")
    void requestNotReservedIsNotReminded() {
        bed.requestStandard();

        assertThat(remindAt(saturday(16, 0))).isZero();
        assertThat(bed.notifier.sent()).isEmpty();
    }
}
