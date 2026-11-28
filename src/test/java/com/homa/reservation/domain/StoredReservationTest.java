package com.homa.reservation.domain;

import static com.homa.fixtures.HomaFixtures.JOB_END;
import static com.homa.fixtures.HomaFixtures.REQUESTED_AT;
import static com.homa.reservation.domain.ReservationFixtures.ACCEPTED_AT;
import static com.homa.reservation.domain.ReservationFixtures.farid;
import static com.homa.reservation.domain.ReservationFixtures.friday;
import static com.homa.reservation.domain.ReservationFixtures.kemasHomeProvider;
import static com.homa.reservation.domain.ReservationFixtures.monday;
import static com.homa.reservation.domain.ReservationFixtures.nurul;
import static com.homa.reservation.domain.ReservationFixtures.saturday;
import static com.homa.reservation.domain.ReservationFixtures.siti;
import static com.homa.reservation.domain.ReservationFixtures.tuesday;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.reservation.domain.ReservationFixtures.Marketplace;
import com.homa.reservation.domain.exception.AcceptanceDeadlinePassedException;
import com.homa.reservation.domain.exception.ReservationStatusConflictException;
import com.homa.reservation.domain.roster.Roster;
import com.homa.reservation.domain.terms.AcceptanceDeadline;
import com.homa.reservation.domain.terms.AcceptancePolicy;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class StoredReservationTest {

    private static final CompletionPhoto CLEAN_KITCHEN = new CompletionPhoto("photo-clean-kitchen");

    /** What a store keeps of a reservation, changeable one part at a time to make it inconsistent. */
    private record Stored(Reservation from, AcceptanceDeadline deadline, ReservationStatus status, Instant reservedAt,
            Instant withdrawnAt, Instant declinedAt, Instant expiredAt, Roster roster, Instant completedAt,
            List<CompletionPhoto> photos, long version) {

        static Stored of(Reservation reservation) {
            return new Stored(reservation, reservation.acceptanceDeadline().orElse(null), reservation.status(),
                    reservation.reservedAt().orElse(null), reservation.withdrawnAt().orElse(null),
                    reservation.declinedAt().orElse(null), reservation.expiredAt().orElse(null),
                    reservation.roster().orElse(null), reservation.completedAt().orElse(null),
                    reservation.completionPhotos(), reservation.version());
        }

        Stored status(ReservationStatus changed) {
            return new Stored(from, deadline, changed, reservedAt, withdrawnAt, declinedAt, expiredAt, roster, completedAt, photos, version);
        }

        Stored deadline(AcceptanceDeadline changed) {
            return new Stored(from, changed, status, reservedAt, withdrawnAt, declinedAt, expiredAt, roster, completedAt, photos, version);
        }

        Stored reservedAt(Instant changed) {
            return new Stored(from, deadline, status, changed, withdrawnAt, declinedAt, expiredAt, roster, completedAt, photos, version);
        }

        Stored declinedAt(Instant changed) {
            return new Stored(from, deadline, status, reservedAt, withdrawnAt, changed, expiredAt, roster, completedAt, photos, version);
        }

        Stored expiredAt(Instant changed) {
            return new Stored(from, deadline, status, reservedAt, withdrawnAt, declinedAt, changed, roster, completedAt, photos, version);
        }

        Stored withdrawnAt(Instant changed) {
            return new Stored(from, deadline, status, reservedAt, changed, declinedAt, expiredAt, roster, completedAt, photos, version);
        }

        Stored roster(Roster changed) {
            return new Stored(from, deadline, status, reservedAt, withdrawnAt, declinedAt, expiredAt, changed, completedAt, photos, version);
        }

        Stored completedAt(Instant changed) {
            return new Stored(from, deadline, status, reservedAt, withdrawnAt, declinedAt, expiredAt, roster, changed, photos, version);
        }

        Stored photos(List<CompletionPhoto> changed) {
            return new Stored(from, deadline, status, reservedAt, withdrawnAt, declinedAt, expiredAt, roster, completedAt, changed, version);
        }

        Stored version(long changed) {
            return new Stored(from, deadline, status, reservedAt, withdrawnAt, declinedAt, expiredAt, roster, completedAt, photos, changed);
        }

        Reservation restore() {
            return Reservation.restore(from.id(), from.checkoutId(), from.booking(), from.terms(), from.quote(),
                    from.values(), from.requestedAt(), deadline, status, reservedAt, withdrawnAt, declinedAt, expiredAt,
                    roster, completedAt, photos, version);
        }
    }

    private Reservation rosteredStandard() {
        Reservation reservation = Marketplace.withKemasHome().reserveStandard();
        reservation.assignRoster(List.of(siti(), nurul()), friday(19, 0));
        return reservation;
    }

    private Reservation completedStandard() {
        Reservation reservation = rosteredStandard();
        reservation.markCompleted(List.of(CLEAN_KITCHEN), JOB_END);
        return reservation;
    }

    private Reservation withdrawnStandard() {
        Reservation reservation = Marketplace.withKemasHome().requestStandard();
        reservation.withdraw(monday(21, 0));
        return reservation;
    }

    private static Roster sitiAndNurul() {
        return Roster.of(List.of(siti().asRosterMember(), nurul().asRosterMember()), 2);
    }

    private static void assertRefused(Stored stored) {
        assertThatThrownBy(stored::restore)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("A stored reservation needs ");
    }

    @Test
    @Tag("REQ-5")
    @DisplayName("Persistence: Aisyah's Request comes back as it was saved, with the version it was saved at, and Kemas Home can still accept it until its deadline.")
    void aStoredRequestComesBackWithItsVersion() {
        Reservation requested = Marketplace.withKemasHome().requestStandard();

        Reservation stored = Stored.of(requested).version(3).restore();

        assertThat(requested.version()).isZero();
        assertThat(stored.version()).isEqualTo(3);
        assertThat(stored).usingRecursiveComparison().ignoringFields("version").isEqualTo(requested);
        Reservation late = Stored.of(requested).restore();
        assertThatThrownBy(() -> late.accept(tuesday(21, 0))).isInstanceOf(AcceptanceDeadlinePassedException.class);
        stored.accept(ACCEPTED_AT);
        assertThat(stored.status()).isEqualTo(ReservationStatus.RESERVED);
        assertThat(stored.reservedAt()).contains(ACCEPTED_AT);
        assertThat(stored.version()).isEqualTo(3);
    }

    @Test
    @Tag("ROS-1")
    @DisplayName("Persistence: Kemas Home's Reserved reservation comes back with its roster, which it can change before the Check-in Period, and is marked Completed after the job.")
    void aStoredReservedReservationKeepsItsRoster() {
        Reservation rostered = rosteredStandard();

        Reservation stored = Stored.of(rostered).restore();

        assertThat(stored).usingRecursiveComparison().isEqualTo(rostered);
        stored.assignRoster(List.of(siti(), nurul(), farid()), friday(20, 0));
        assertThat(stored.roster().orElseThrow().members()).hasSize(3);
        assertThat(stored.completionRemindersDueAt(JOB_END)).containsExactly(JOB_END);
        stored.markCompleted(List.of(CLEAN_KITCHEN), JOB_END);
        assertThat(stored.status()).isEqualTo(ReservationStatus.COMPLETED);
    }

    @Test
    @Tag("JOB-6")
    @DisplayName("Persistence: a Completed reservation comes back with its after-photo, and can't be marked Completed again.")
    void aStoredCompletedReservationKeepsItsAfterPhotos() {
        Reservation completed = completedStandard();

        Reservation stored = Stored.of(completed).restore();

        assertThat(stored).usingRecursiveComparison().isEqualTo(completed);
        assertThat(stored.completionPhotos()).containsExactly(CLEAN_KITCHEN);
        assertThat(stored.completionRemindersDueAt(saturday(16, 0))).isEmpty();
        assertThatThrownBy(() -> stored.markCompleted(List.of(CLEAN_KITCHEN), saturday(16, 0)))
                .isInstanceOf(ReservationStatusConflictException.class);
    }

    @Test
    @Tag("REQ-2")
    @DisplayName("Persistence: Aisyah's withdrawn Request comes back Withdrawn, and Kemas Home can no longer accept it.")
    void aStoredWithdrawnReservationStaysWithdrawn() {
        Reservation withdrawn = withdrawnStandard();

        Reservation stored = Stored.of(withdrawn).restore();

        assertThat(stored).usingRecursiveComparison().isEqualTo(withdrawn);
        assertThatThrownBy(() -> stored.accept(ACCEPTED_AT)).isInstanceOf(ReservationStatusConflictException.class);
    }

    @Test
    @Tag("ACC-1")
    @DisplayName("Persistence: under Instant the reservation comes back Reserved from the moment it was Requested, with no acceptance deadline.")
    void aStoredInstantReservationHasNoAcceptanceDeadline() {
        Reservation instant = new Marketplace(kemasHomeProvider().policy(AcceptancePolicy.INSTANT).build())
                .requestStandard();

        Reservation stored = Stored.of(instant).restore();

        assertThat(stored).usingRecursiveComparison().isEqualTo(instant);
        assertThat(stored.acceptanceDeadline()).isEmpty();
        assertThat(stored.reservedAt()).contains(REQUESTED_AT);
        assertRefused(Stored.of(instant).deadline(new AcceptanceDeadline(tuesday(20, 0))));
    }

    @Test
    @Tag("REQ-5")
    @DisplayName("Persistence: a stored status must fit when the reservation was reserved, withdrawn and completed.")
    void aStoredStatusThatDoesNotFitItsMomentsIsRefused() {
        Stored requested = Stored.of(Marketplace.withKemasHome().requestStandard());
        Stored reserved = Stored.of(Marketplace.withKemasHome().reserveStandard());
        Stored completed = Stored.of(completedStandard());
        Stored withdrawn = Stored.of(withdrawnStandard());

        assertRefused(requested.reservedAt(ACCEPTED_AT));
        assertRefused(requested.withdrawnAt(monday(21, 0)));
        assertRefused(requested.completedAt(JOB_END));
        assertRefused(reserved.reservedAt(null));
        assertRefused(reserved.status(ReservationStatus.REQUESTED));
        assertRefused(completed.completedAt(null).photos(List.of()));
        assertRefused(completed.reservedAt(null));
        assertRefused(withdrawn.withdrawnAt(null));
        assertRefused(withdrawn.reservedAt(ACCEPTED_AT));
        assertRefused(reserved.status(ReservationStatus.CANCELLED));
    }

    @Test
    @Tag("REQ-3")
    @DisplayName("Persistence: Kemas Home's declined Request comes back Declined, and it can no longer be accepted.")
    void aStoredDeclinedReservationStaysDeclined() {
        Reservation declined = Marketplace.withKemasHome().requestStandard();
        declined.decline(monday(21, 0));

        Reservation stored = Stored.of(declined).restore();

        assertThat(stored).usingRecursiveComparison().isEqualTo(declined);
        assertThat(stored.declinedAt()).contains(monday(21, 0));
        assertThatThrownBy(() -> stored.accept(ACCEPTED_AT)).isInstanceOf(ReservationStatusConflictException.class);
    }

    @Test
    @Tag("REQ-4")
    @DisplayName("Persistence: Aisyah's expired Request comes back Expired, and it can no longer be accepted.")
    void aStoredExpiredReservationStaysExpired() {
        Reservation expired = Marketplace.withKemasHome().requestStandard();
        expired.expire(tuesday(20, 0));

        Reservation stored = Stored.of(expired).restore();

        assertThat(stored).usingRecursiveComparison().isEqualTo(expired);
        assertThat(stored.expiredAt()).contains(tuesday(20, 0));
        assertThatThrownBy(() -> stored.accept(ACCEPTED_AT)).isInstanceOf(ReservationStatusConflictException.class);
    }

    @Test
    @Tag("REQ-4")
    @DisplayName("Persistence: a stored status must fit when the reservation was declined and expired.")
    void aStoredDeclinedOrExpiredStatusThatDoesNotFitItsMomentsIsRefused() {
        Reservation declinedReservation = Marketplace.withKemasHome().requestStandard();
        declinedReservation.decline(monday(21, 0));
        Reservation expiredReservation = Marketplace.withKemasHome().requestStandard();
        expiredReservation.expire(tuesday(20, 0));
        Stored requested = Stored.of(Marketplace.withKemasHome().requestStandard());
        Stored declined = Stored.of(declinedReservation);
        Stored expired = Stored.of(expiredReservation);

        assertRefused(requested.declinedAt(monday(21, 0)));
        assertRefused(requested.expiredAt(tuesday(20, 0)));
        assertRefused(declined.declinedAt(null));
        assertRefused(declined.expiredAt(tuesday(20, 0)));
        assertRefused(expired.expiredAt(null));
        assertRefused(expired.declinedAt(monday(21, 0)));
        assertRefused(expired.withdrawnAt(monday(21, 0)));
    }

    @Test
    @Tag("ACC-3")
    @DisplayName("Persistence: a stored Request needs its acceptance deadline.")
    void aStoredRequestWithoutItsDeadlineIsRefused() {
        assertRefused(Stored.of(Marketplace.withKemasHome().requestStandard()).deadline(null));
    }

    @Test
    @Tag("JOB-3")
    @DisplayName("Persistence: a stored Completed reservation needs its after-photos, and only a Completed one has any.")
    void storedAfterPhotosMustFitTheCompletion() {
        assertRefused(Stored.of(completedStandard()).photos(List.of()));
        assertRefused(Stored.of(Marketplace.withKemasHome().reserveStandard()).photos(List.of(CLEAN_KITCHEN)));
    }

    @Test
    @Tag("ROS-1")
    @DisplayName("Persistence: only a reservation that was Reserved can have a stored roster.")
    void aStoredRosterNeedsAReservedReservation() {
        assertRefused(Stored.of(Marketplace.withKemasHome().requestStandard()).roster(sitiAndNurul()));
        assertRefused(Stored.of(withdrawnStandard()).roster(sitiAndNurul()));
        assertThat(Stored.of(completedStandard()).roster(null).restore().roster()).isEmpty();
    }

    @Test
    @Tag("RES-7")
    @DisplayName("Persistence: a stored reservation needs its id, checkout, booking, terms, price, values, request time, status and photos.")
    void aStoredReservationMissingAPartIsRefused() {
        Reservation from = Marketplace.withKemasHome().requestStandard();
        AcceptanceDeadline deadline = from.acceptanceDeadline().orElseThrow();

        assertThatThrownBy(() -> Reservation.restore(null, from.checkoutId(), from.booking(), from.terms(),
                from.quote(), from.values(), REQUESTED_AT, deadline, ReservationStatus.REQUESTED, null, null, null, null, null, null,
                List.of(), 0)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Reservation.restore(from.id(), null, from.booking(), from.terms(), from.quote(),
                from.values(), REQUESTED_AT, deadline, ReservationStatus.REQUESTED, null, null, null, null, null, null,
                List.of(),
                0)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Reservation.restore(from.id(), from.checkoutId(), null, from.terms(), from.quote(),
                from.values(), REQUESTED_AT, deadline, ReservationStatus.REQUESTED, null, null, null, null, null, null,
                List.of(),
                0)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Reservation.restore(from.id(), from.checkoutId(), from.booking(), null,
                from.quote(), from.values(), REQUESTED_AT, deadline, ReservationStatus.REQUESTED, null, null, null, null, null, null,
                List.of(), 0)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Reservation.restore(from.id(), from.checkoutId(), from.booking(), from.terms(),
                null, from.values(), REQUESTED_AT, deadline, ReservationStatus.REQUESTED, null, null, null, null, null, null,
                List.of(), 0)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Reservation.restore(from.id(), from.checkoutId(), from.booking(), from.terms(),
                from.quote(), null, REQUESTED_AT, deadline, ReservationStatus.REQUESTED, null, null, null, null, null, null,
                List.of(), 0)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Reservation.restore(from.id(), from.checkoutId(), from.booking(), from.terms(),
                from.quote(), from.values(), null, deadline, ReservationStatus.REQUESTED, null, null, null, null, null, null,
                List.of(), 0)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Stored.of(from).status(null).restore()).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Stored.of(from).photos(null).restore()).isInstanceOf(NullPointerException.class);
    }
}
