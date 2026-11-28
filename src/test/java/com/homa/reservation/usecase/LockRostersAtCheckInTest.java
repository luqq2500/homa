package com.homa.reservation.usecase;

import static com.homa.fixtures.HomaFixtures.AISYAH_CUSTOMER_ID;
import static com.homa.fixtures.HomaFixtures.KEMAS_HOME_PROVIDER_ID;
import static com.homa.fixtures.HomaFixtures.NURUL_MEMBER_ID;
import static com.homa.fixtures.HomaFixtures.REQUESTED_AT;
import static com.homa.fixtures.HomaFixtures.SITI_MEMBER_ID;
import static com.homa.reservation.usecase.ReservationTestBed.AZMAN_PERSON_ID;
import static com.homa.reservation.usecase.ReservationTestBed.HANA_PERSON_ID;
import static com.homa.reservation.usecase.ReservationTestBed.INSTANT_PROVIDER_ID;
import static com.homa.reservation.usecase.ReservationTestBed.MEI_CUSTOMER_ID;
import static com.homa.reservation.usecase.ReservationTestBed.NURUL_PERSON_ID;
import static com.homa.reservation.usecase.ReservationTestBed.friday;
import static com.homa.reservation.usecase.ReservationTestBed.saturday;
import static com.homa.reservation.usecase.ReservationTestBed.sunday;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.ie.events.ProfilePhotoChangedIE;
import com.homa.ie.events.ReservationCancelledIE;
import com.homa.kernel.Money;
import com.homa.reservation.domain.Reservation;
import com.homa.reservation.domain.ReservationStatus;
import com.homa.reservation.usecase.dto.AssignRosterRequest;
import com.homa.reservation.usecase.dto.CustomerCancelReservationRequest;
import com.homa.reservation.usecase.dto.LockRosterRequest;
import com.homa.reservation.usecase.dto.LockRosterResponse;
import com.homa.reservation.usecase.dto.LockRostersAtCheckInRequest;
import com.homa.reservation.usecase.dto.LockRostersAtCheckInResponse;
import com.homa.reservation.usecase.exception.ReservationNotFoundException;
import com.homa.reservation.usecase.exception.ScheduleChangedException;
import com.homa.reservation.usecase.port.in.RosterLocker;
import com.homa.reservation.usecase.port.out.ReservationChangedException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class LockRostersAtCheckInTest {

    private final ReservationTestBed bed = ReservationTestBed.withKemasHomeAndAisyah();
    private final Reservation reserved = bed.reserveStandard();

    private void rosterSitiAndNurul() {
        bed.time.set(friday(18, 0));
        bed.assignRoster.assign(new AssignRosterRequest(reserved.id().value(), AZMAN_PERSON_ID,
                List.of(SITI_MEMBER_ID, NURUL_MEMBER_ID)));
    }

    private void nurulIsSuspendedOnFriday() {
        bed.time.set(friday(20, 0));
        bed.teamMemberDeactivated.handle(bed.deactivation(NURUL_MEMBER_ID, NURUL_PERSON_ID, "DEACTIVATED"));
    }

    private LockRostersAtCheckInResponse run() {
        return bed.lockRostersAtCheckIn.lockRosters(new LockRostersAtCheckInRequest());
    }

    @Test
    @Tag("ROS-7")
    @Tag("NRS-1")
    @Tag("NRS-2")
    @Tag("NRS-3")
    @DisplayName("Nurul is suspended on Friday and not replaced by 9:45. The No Roster Policy applies: Kemas Home is fined RM69 and Aisyah gets RM208.")
    void anIncompleteRosterAtCheckInCancelsTheReservationAndTheTimeOpens() {
        rosterSitiAndNurul();
        nurulIsSuspendedOnFriday();
        bed.publisher.clear();
        bed.time.set(saturday(9, 45));

        LockRostersAtCheckInResponse response = run();

        String id = reserved.id().value();
        assertThat(response).isEqualTo(new LockRostersAtCheckInResponse(List.of(), List.of(id), List.of()));
        Reservation saved = bed.reload(reserved);
        assertThat(saved.status()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(saved.cancellation().orElseThrow().cause().name()).isEqualTo("NO_ROSTER");
        assertThat(bed.schedules.providerBlocks(KEMAS_HOME_PROVIDER_ID)).isEmpty();
        assertThat(bed.schedules.customerBlocks(AISYAH_CUSTOMER_ID)).isEmpty();
        assertThat(bed.publisher.single(ReservationCancelledIE.class)).isEqualTo(new ReservationCancelledIE(
                "reservation-event-6", id, AISYAH_CUSTOMER_ID, KEMAS_HOME_PROVIDER_ID, "NO_ROSTER", saturday(9, 45),
                REQUESTED_AT, saturday(9, 45), sunday(14, 0), "settlement-" + id, Money.myr(0), Money.myr(200),
                Money.myr(8), Money.myr(69), Money.myr(0), saturday(9, 45)));
    }

    @Test
    @Tag("NRS-1")
    @DisplayName("Kemas Home never rostered anyone: at 9:45 the booking is cancelled and Aisyah is told at once.")
    void aReservationWithNoRosterAtAllIsCancelledAtCheckIn() {
        bed.publisher.clear();
        bed.time.set(saturday(9, 45));

        LockRostersAtCheckInResponse response = run();

        assertThat(response.cancelledReservationIds()).containsExactly(reserved.id().value());
        assertThat(bed.publisher.single(ReservationCancelledIE.class).customerId()).isEqualTo(AISYAH_CUSTOMER_ID);
        assertThat(bed.reload(reserved).status()).isEqualTo(ReservationStatus.CANCELLED);
    }

    @Test
    @Tag("ROS-1")
    @DisplayName("Kemas Home rosters Siti and Nurul on Friday, and the roster locks at 9:45: the reservation goes on and nothing is published.")
    void aCompleteRosterLocksAtCheckIn() {
        rosterSitiAndNurul();
        bed.publisher.clear();
        bed.time.set(saturday(9, 45));

        LockRostersAtCheckInResponse response = run();

        assertThat(response).isEqualTo(
                new LockRostersAtCheckInResponse(List.of(reserved.id().value()), List.of(), List.of()));
        Reservation saved = bed.reload(reserved);
        assertThat(saved.status()).isEqualTo(ReservationStatus.RESERVED);
        assertThat(saved.roster().orElseThrow().lockedAt()).contains(saturday(9, 45));
        assertThat(bed.schedules.providerBlocks(KEMAS_HOME_PROVIDER_ID)).hasSize(1);
        assertThat(bed.publisher.published()).isEmpty();
    }

    @Test
    @Tag("ROS-1")
    @DisplayName("At 9:44 the roster can still change, so nothing locks and nothing is cancelled.")
    void nothingLocksBeforeTheCheckInPeriod() {
        bed.publisher.clear();
        bed.time.set(saturday(9, 44));

        LockRostersAtCheckInResponse response = run();

        assertThat(response).isEqualTo(new LockRostersAtCheckInResponse(List.of(), List.of(), List.of()));
        assertThat(bed.reload(reserved).status()).isEqualTo(ReservationStatus.RESERVED);
        assertThat(bed.publisher.published()).isEmpty();
    }

    @Test
    @Tag("ROS-7")
    @DisplayName("At 9:46 the roster has locked or the reservation is cancelled already: the next run finds nothing to do and publishes nothing.")
    void aRunAfterTheLockChangesAndPublishesNothing() {
        bed.time.set(saturday(9, 45));
        run();
        bed.publisher.clear();
        bed.time.set(saturday(9, 46));

        LockRostersAtCheckInResponse repeated = run();

        assertThat(repeated).isEqualTo(new LockRostersAtCheckInResponse(List.of(), List.of(), List.of()));
        assertThat(bed.publisher.published()).isEmpty();
    }

    @Test
    @Tag("ROS-1")
    @DisplayName("A run that comes after a complete roster locked doesn't lock it again, and the lock keeps the moment it was made.")
    void aLockedRosterIsNotLockedAgainByALaterRun() {
        rosterSitiAndNurul();
        bed.time.set(saturday(9, 45));
        run();
        bed.time.set(saturday(9, 46));

        LockRostersAtCheckInResponse repeated = run();

        assertThat(repeated).isEqualTo(new LockRostersAtCheckInResponse(List.of(), List.of(), List.of()));
        assertThat(bed.reload(reserved).roster().orElseThrow().lockedAt()).contains(saturday(9, 45));
    }

    @Test
    @Tag("ROS-7")
    @DisplayName("Kemas Home's reservation has no complete roster and Hana Cleaning's has: one run cancels the first and locks the second.")
    void eachDueReservationIsLockedOrCancelledOnItsOwn() {
        bed.listInstantProvider();
        String checkoutId = bed.startCheckout(MEI_CUSTOMER_ID, INSTANT_PROVIDER_ID);
        bed.time.set(REQUESTED_AT);
        bed.paymentHeld.handle(bed.paymentHeld(checkoutId, REQUESTED_AT));
        Reservation meis = bed.reservations.findByParty(MEI_CUSTOMER_ID).get(0);
        bed.profilePhotoChanged.handle(new ProfilePhotoChangedIE(bed.nextInboundId(), "person-hana-2", true,
                "photo-hana-2", REQUESTED_AT));
        bed.assignRoster.assign(new AssignRosterRequest(meis.id().value(), HANA_PERSON_ID,
                List.of("member-hana", "member-hana-2")));
        bed.publisher.clear();
        bed.time.set(saturday(9, 45));

        LockRostersAtCheckInResponse response = run();

        assertThat(response.cancelledReservationIds()).containsExactly(reserved.id().value());
        assertThat(response.lockedReservationIds()).containsExactly(meis.id().value());
        assertThat(response.skippedReservationIds()).isEmpty();
        assertThat(bed.reload(reserved).status()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(bed.reload(meis).roster().orElseThrow().isLocked()).isTrue();
        assertThat(bed.publisher.single(ReservationCancelledIE.class).reservationId()).isEqualTo(reserved.id().value());
    }

    @Test
    @Tag("ROS-7")
    @DisplayName("When another scheduler task cancels the reservation while this run does, it is cancelled and published once.")
    void aReservationAnotherTaskLockedFirstIsSkippedAndPublishedOnce() {
        bed.publisher.clear();
        bed.time.set(saturday(9, 45));
        bed.reservations.beforeNextSave(() -> bed.lockRoster.lock(new LockRosterRequest(reserved.id().value())));

        LockRostersAtCheckInResponse response = run();

        assertThat(response).isEqualTo(
                new LockRostersAtCheckInResponse(List.of(), List.of(), List.of(reserved.id().value())));
        assertThat(bed.reload(reserved).status()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(bed.publisher.single(ReservationCancelledIE.class).reservationId()).isEqualTo(reserved.id().value());
    }

    @Test
    @Tag("ROS-7")
    @DisplayName("When Aisyah cancels at 9:45 while the run does, whoever is saved first decides: the run skips it and only her cancellation is published.")
    void aCustomersCancellationSavedFirstWinsOverTheLock() {
        bed.publisher.clear();
        bed.time.set(saturday(9, 45));
        bed.reservations.beforeNextSave(() -> bed.customerCancel
                .cancel(new CustomerCancelReservationRequest(reserved.id().value(), AISYAH_CUSTOMER_ID)));

        LockRostersAtCheckInResponse response = run();

        assertThat(response.skippedReservationIds()).containsExactly(reserved.id().value());
        assertThat(bed.reload(reserved).cancellation().orElseThrow().cause().name()).isEqualTo("CUSTOMER_LAST_MINUTE");
        assertThat(bed.publisher.single(ReservationCancelledIE.class).cause()).isEqualTo("CUSTOMER_LAST_MINUTE");
    }

    @Test
    @Tag("ROS-7")
    @DisplayName("A reservation whose cancellation keeps conflicting is skipped, and the next run cancels it.")
    void aReservationWhoseCancellationKeepsConflictingIsCancelledByTheNextRun() {
        bed.publisher.clear();
        bed.time.set(saturday(9, 45));
        bed.reservations.conflictOnNextSaves(3);

        LockRostersAtCheckInResponse stuck = run();

        assertThat(stuck.skippedReservationIds()).containsExactly(reserved.id().value());
        assertThat(bed.publisher.published()).isEmpty();

        bed.time.set(saturday(9, 46));
        assertThat(run().cancelledReservationIds()).containsExactly(reserved.id().value());
        assertThat(bed.publisher.single(ReservationCancelledIE.class).cancelledAt()).isEqualTo(saturday(9, 46));
    }

    @Test
    @Tag("ROS-1")
    @DisplayName("Each due reservation is locked through its own unit of work; one that changed under the run is skipped and the others still lock.")
    void eachDueReservationIsItsOwnUnitOfWork() {
        bed.time.set(saturday(9, 45));
        List<String> asked = new ArrayList<>();
        RosterLocker locker = request -> {
            asked.add(request.reservationId());
            throw new ReservationChangedException(request.reservationId());
        };

        LockRostersAtCheckInResponse response =
                new LockRostersAtCheckIn(bed.reservations, locker, bed.time).lockRosters(new LockRostersAtCheckInRequest());

        assertThat(asked).containsExactly(reserved.id().value());
        assertThat(response.skippedReservationIds()).containsExactly(reserved.id().value());
        assertThat(bed.reload(reserved).status()).isEqualTo(ReservationStatus.RESERVED);
    }

    @Test
    @Tag("ROS-1")
    @DisplayName("A schedule that keeps changing under a cancellation skips that reservation for this run, and a failure of any other kind ends the run.")
    void aChangingScheduleIsSkippedButOtherFailuresEndTheRun() {
        bed.time.set(saturday(9, 45));
        RosterLocker scheduleChanges = request -> {
            throw new ScheduleChangedException(KEMAS_HOME_PROVIDER_ID);
        };
        RosterLocker breaks = request -> {
            throw new IllegalStateException("database down");
        };

        LockRostersAtCheckInResponse skipped = new LockRostersAtCheckIn(bed.reservations, scheduleChanges, bed.time)
                .lockRosters(new LockRostersAtCheckInRequest());

        assertThat(skipped.skippedReservationIds()).containsExactly(reserved.id().value());
        assertThatThrownBy(() -> new LockRostersAtCheckIn(bed.reservations, breaks, bed.time)
                .lockRosters(new LockRostersAtCheckInRequest())).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @Tag("ROS-7")
    @DisplayName("Locking one reservation again after it was cancelled for no roster changes nothing and publishes nothing; an unknown reservation is refused.")
    void lockingOneReservationTwiceIsRefusedAndRepeatsNothing() {
        bed.time.set(saturday(9, 45));
        LockRosterResponse first = bed.lockRoster.lock(new LockRosterRequest(reserved.id().value()));
        bed.publisher.clear();

        assertThat(first).isEqualTo(new LockRosterResponse(reserved.id().value(), true, saturday(9, 45)));
        assertThatThrownBy(() -> bed.lockRoster.lock(new LockRosterRequest(reserved.id().value())))
                .isInstanceOf(com.homa.reservation.domain.exception.ReservationStatusConflictException.class);
        assertThat(bed.publisher.published()).isEmpty();
        assertThatThrownBy(() -> bed.lockRoster.lock(new LockRosterRequest("reservation-unknown")))
                .isInstanceOf(ReservationNotFoundException.class);
    }

    @Test
    @Tag("ROS-1")
    @DisplayName("A roster locked at 9:45 is reported as locked, not cancelled, with the moment it locked.")
    void aLockedRosterIsReportedAsLocked() {
        rosterSitiAndNurul();
        bed.time.set(saturday(9, 45));

        LockRosterResponse response = bed.lockRoster.lock(new LockRosterRequest(reserved.id().value()));

        assertThat(response).isEqualTo(new LockRosterResponse(reserved.id().value(), false, saturday(9, 45)));
    }
}
