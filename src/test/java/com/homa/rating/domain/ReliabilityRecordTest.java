package com.homa.rating.domain;

import static com.homa.fixtures.HomaFixtures.BASE_CANCELLATION_FEE;
import static com.homa.fixtures.HomaFixtures.at;
import static com.homa.rating.domain.RatingFixtures.AISYAH_PARTY;
import static com.homa.rating.domain.RatingFixtures.KEMAS_HOME_PARTY;
import static com.homa.rating.domain.RatingFixtures.RESERVATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.kernel.Money;
import com.homa.rating.domain.exception.IncidentNotCountedException;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class ReliabilityRecordTest {

    private static final Instant FRIDAY_22_00 = at(2026, 12, 4, 22, 0);
    private static final Instant TWELVE_MONTHS_LATER = at(2027, 12, 4, 22, 0);
    private static final Money NOTHING = Money.myr(0);

    private static Cancellation cancelled(String reservation, CancellationCause cause, Money fee, Money fine,
            Instant at) {
        return new Cancellation(new ReservationId(reservation), cause, fee, fine, at);
    }

    @Test
    @Tag("RTG-10")
    @DisplayName("A party nobody has logged anything about shows 0 everywhere.")
    void aNewRecordShowsNoIncidents() {
        ReliabilityRecord record = ReliabilityRecord.openFor(KEMAS_HOME_PARTY);

        assertThat(record.countsAt(FRIDAY_22_00)).isEqualTo(new ReliabilityCounts(0, 0, 0, 0));
        assertThat(record.party()).isEqualTo(KEMAS_HOME_PARTY);
        assertThat(record.incidents()).isEmpty();
        assertThat(record.version()).isZero();
    }

    @Test
    @Tag("RTG-10")
    @DisplayName("Kemas Home shows \"Cancellations: 1, No-rosters: 0, No-shows: 0\" after one cancellation.")
    void aProviderShowsItsCancellations() {
        ReliabilityRecord record = ReliabilityRecord.openFor(KEMAS_HOME_PARTY);

        boolean logged = record.recordCancellation(cancelled("reservation-1", CancellationCause.PROVIDER_CANCELLED,
                NOTHING, BASE_CANCELLATION_FEE, FRIDAY_22_00));

        assertThat(logged).isTrue();
        assertThat(record.countsAt(FRIDAY_22_00)).isEqualTo(new ReliabilityCounts(1, 0, 0, 0));
        assertThat(record.incidents()).containsExactly(
                Incident.of(IncidentKind.CANCELLATION, new ReservationId("reservation-1"), FRIDAY_22_00));
    }

    @Test
    @Tag("RTG-10")
    @DisplayName("Customers see a provider's cancellations, no-rosters, no-shows and members who left, each counted "
            + "on its own.")
    void aProviderShowsEveryKindOnItsOwn() {
        ReliabilityRecord record = ReliabilityRecord.openFor(KEMAS_HOME_PARTY);
        record.recordCancellation(cancelled("r-1", CancellationCause.PROVIDER_CANCELLED, NOTHING, Money.myr(45),
                FRIDAY_22_00));
        record.recordCancellation(cancelled("r-2", CancellationCause.PROVIDER_LAST_MINUTE, NOTHING, Money.myr(85),
                FRIDAY_22_00));
        record.recordCancellation(cancelled("r-3", CancellationCause.NO_ROSTER, NOTHING, Money.myr(69), FRIDAY_22_00));
        record.recordCancellation(cancelled("r-4", CancellationCause.PROVIDER_NO_SHOW, NOTHING, Money.myr(105),
                FRIDAY_22_00));
        record.recordCancellation(cancelled("r-5", CancellationCause.PROVIDER_NO_SHOW, NOTHING, Money.myr(105),
                FRIDAY_22_00));
        record.recordMemberLeft(new ReservationId("r-6"), "member-nurul", FRIDAY_22_00);
        record.recordMemberLeft(new ReservationId("r-6"), "member-siti", FRIDAY_22_00);
        record.recordMemberLeft(new ReservationId("r-7"), "member-nurul", FRIDAY_22_00);
        record.recordMemberLeft(new ReservationId("r-7"), "member-rina", FRIDAY_22_00);
        record.recordMemberLeft(new ReservationId("r-7"), "member-farid", FRIDAY_22_00);

        assertThat(record.countsAt(FRIDAY_22_00)).isEqualTo(new ReliabilityCounts(2, 1, 2, 5));
    }

    @Test
    @Tag("RTG-10")
    @DisplayName("Providers see a customer's fee-paying cancellations and no-shows: Aisyah's free cancellation isn't "
            + "counted, her RM45 one is.")
    void aCustomerShowsFeePayingCancellationsAndNoShows() {
        ReliabilityRecord record = ReliabilityRecord.openFor(AISYAH_PARTY);

        boolean free = record.recordCancellation(cancelled("r-1", CancellationCause.CUSTOMER_CANCELLED, NOTHING,
                NOTHING, FRIDAY_22_00));
        boolean paid = record.recordCancellation(cancelled("r-2", CancellationCause.CUSTOMER_CANCELLED,
                BASE_CANCELLATION_FEE, NOTHING, FRIDAY_22_00));
        boolean noShow = record.recordCancellation(cancelled("r-3", CancellationCause.CUSTOMER_NO_SHOW,
                Money.myr("105.50"), NOTHING, FRIDAY_22_00));

        assertThat(free).isFalse();
        assertThat(paid).isTrue();
        assertThat(noShow).isTrue();
        assertThat(record.countsAt(FRIDAY_22_00)).isEqualTo(new ReliabilityCounts(1, 0, 1, 0));
    }

    @Test
    @Tag("RTG-10")
    @DisplayName("A provider's cancellation never counts against Aisyah, and hers never against Kemas Home.")
    void aCancellationCountsOnlyAgainstItsOwnSide() {
        ReliabilityRecord aisyah = ReliabilityRecord.openFor(AISYAH_PARTY);
        ReliabilityRecord kemasHome = ReliabilityRecord.openFor(KEMAS_HOME_PARTY);
        Cancellation byProvider = cancelled("r-1", CancellationCause.PROVIDER_CANCELLED, NOTHING, Money.myr(45),
                FRIDAY_22_00);
        Cancellation byCustomer = cancelled("r-2", CancellationCause.CUSTOMER_CANCELLED, Money.myr(45), NOTHING,
                FRIDAY_22_00);

        assertThat(aisyah.recordCancellation(byProvider)).isFalse();
        assertThat(kemasHome.recordCancellation(byCustomer)).isFalse();
        assertThat(aisyah.countsAt(FRIDAY_22_00)).isEqualTo(new ReliabilityCounts(0, 0, 0, 0));
        assertThat(kemasHome.countsAt(FRIDAY_22_00)).isEqualTo(new ReliabilityCounts(0, 0, 0, 0));
    }

    @Test
    @Tag("RTG-10")
    @DisplayName("Aisyah's RM45 cancellation on Friday 22:00 counts for 12 months: one minute before 4 December the "
            + "next year it still counts.")
    void anIncidentCountsUntilTwelveMonthsHavePassed() {
        ReliabilityRecord record = ReliabilityRecord.openFor(AISYAH_PARTY);
        record.recordCancellation(cancelled("r-1", CancellationCause.CUSTOMER_CANCELLED, BASE_CANCELLATION_FEE,
                NOTHING, FRIDAY_22_00));

        assertThat(record.countsAt(FRIDAY_22_00)).isEqualTo(new ReliabilityCounts(1, 0, 0, 0));
        assertThat(record.countsAt(TWELVE_MONTHS_LATER.minus(Duration.ofMinutes(1))).cancellations()).isEqualTo(1);
    }

    @Test
    @Tag("RTG-10")
    @Tag("DAT-4")
    @DisplayName("An incident exactly 12 months old stops counting, and so does one a minute older.")
    void anIncidentExactlyTwelveMonthsOldNoLongerCounts() {
        ReliabilityRecord record = ReliabilityRecord.openFor(AISYAH_PARTY);
        record.recordCancellation(cancelled("r-1", CancellationCause.CUSTOMER_CANCELLED, BASE_CANCELLATION_FEE,
                NOTHING, FRIDAY_22_00));

        assertThat(record.countsAt(TWELVE_MONTHS_LATER).cancellations()).isZero();
        assertThat(record.countsAt(TWELVE_MONTHS_LATER.plus(Duration.ofMinutes(1))).cancellations()).isZero();
        assertThat(record.incidents()).hasSize(1);
    }

    @Test
    @Tag("RTG-10")
    @DisplayName("Only the old incident leaves the count: a newer one logged in March still counts in December.")
    void onlyTheOldIncidentLeavesTheCount() {
        ReliabilityRecord record = ReliabilityRecord.openFor(KEMAS_HOME_PARTY);
        record.recordCancellation(cancelled("r-1", CancellationCause.PROVIDER_CANCELLED, NOTHING, Money.myr(45),
                FRIDAY_22_00));
        record.recordCancellation(cancelled("r-2", CancellationCause.PROVIDER_CANCELLED, NOTHING, Money.myr(45),
                at(2027, 3, 1, 10, 0)));

        assertThat(record.countsAt(TWELVE_MONTHS_LATER).cancellations()).isEqualTo(1);
    }

    @Test
    @Tag("RTG-10")
    @DisplayName("Months are calendar months in Kuala Lumpur: an incident on 29 February 2028 stops counting on "
            + "28 February 2029.")
    void monthsAreCountedOnTheCalendar() {
        ReliabilityRecord record = ReliabilityRecord.openFor(KEMAS_HOME_PARTY);
        record.recordMemberLeft(RESERVATION, "member-nurul", at(2028, 2, 29, 10, 0));

        assertThat(record.countsAt(at(2029, 2, 28, 9, 59)).membersLeft()).isEqualTo(1);
        assertThat(record.countsAt(at(2029, 2, 28, 10, 0)).membersLeft()).isZero();
    }

    @Test
    @Tag("RTG-10")
    @DisplayName("The same cancellation logged twice counts once.")
    void theSameIncidentIsLoggedOnce() {
        ReliabilityRecord record = ReliabilityRecord.openFor(KEMAS_HOME_PARTY);
        Cancellation cancellation = cancelled("r-1", CancellationCause.PROVIDER_NO_SHOW, NOTHING, Money.myr(105),
                FRIDAY_22_00);

        assertThat(record.recordCancellation(cancellation)).isTrue();
        assertThat(record.recordCancellation(cancellation)).isFalse();

        assertThat(record.incidents()).hasSize(1);
        assertThat(record.countsAt(FRIDAY_22_00).noShows()).isEqualTo(1);
    }

    @Test
    @Tag("RTG-10")
    @DisplayName("A no show and a cancellation on the same reservation are two incidents; the same member leaving "
            + "twice is one.")
    void incidentsAreToldApartByKindAndMember() {
        ReliabilityRecord record = ReliabilityRecord.openFor(KEMAS_HOME_PARTY);

        assertThat(record.recordCancellation(cancelled("r-1", CancellationCause.PROVIDER_NO_SHOW, NOTHING,
                Money.myr(105), FRIDAY_22_00))).isTrue();
        assertThat(record.recordCancellation(cancelled("r-1", CancellationCause.PROVIDER_CANCELLED, NOTHING,
                Money.myr(45), FRIDAY_22_00))).isTrue();
        assertThat(record.recordMemberLeft(new ReservationId("r-1"), "member-nurul", FRIDAY_22_00)).isTrue();
        assertThat(record.recordMemberLeft(new ReservationId("r-1"), "member-nurul", FRIDAY_22_00)).isFalse();
        assertThat(record.recordMemberLeft(new ReservationId("r-1"), "member-siti", FRIDAY_22_00)).isTrue();
        assertThat(record.recordMemberLeft(new ReservationId("r-2"), "member-nurul", FRIDAY_22_00)).isTrue();

        assertThat(record.incidents()).hasSize(5);
    }

    @Test
    @Tag("RTG-10")
    @DisplayName("A customer's reliability has no members who left: Aisyah's record refuses them.")
    void aCustomerHasNoMembersWhoLeft() {
        ReliabilityRecord record = ReliabilityRecord.openFor(AISYAH_PARTY);

        assertThatThrownBy(() -> record.recordMemberLeft(RESERVATION, "member-nurul", FRIDAY_22_00))
                .isInstanceOf(IncidentNotCountedException.class)
                .hasMessageContaining("RTG-10")
                .hasMessageContaining("MEMBER_LEFT")
                .hasMessageContaining("CUSTOMER");
        assertThat(record.incidents()).isEmpty();
    }

    @Test
    @Tag("RTG-10")
    @DisplayName("Which kinds each side can have: both have cancellations and no-shows, only a provider has "
            + "no-rosters and members who left.")
    void eachSideHasItsOwnKinds() {
        assertThat(IncidentKind.CANCELLATION.concerns(PartyRole.CUSTOMER)).isTrue();
        assertThat(IncidentKind.CANCELLATION.concerns(PartyRole.PROVIDER)).isTrue();
        assertThat(IncidentKind.NO_SHOW.concerns(PartyRole.CUSTOMER)).isTrue();
        assertThat(IncidentKind.NO_SHOW.concerns(PartyRole.PROVIDER)).isTrue();
        assertThat(IncidentKind.NO_ROSTER.concerns(PartyRole.CUSTOMER)).isFalse();
        assertThat(IncidentKind.NO_ROSTER.concerns(PartyRole.PROVIDER)).isTrue();
        assertThat(IncidentKind.MEMBER_LEFT.concerns(PartyRole.CUSTOMER)).isFalse();
        assertThat(IncidentKind.MEMBER_LEFT.concerns(PartyRole.PROVIDER)).isTrue();
    }

    @Test
    @Tag("DAT-4")
    @DisplayName("An incident is kept until the moment it stops counting, 12 months after it happened.")
    void anIncidentIsKeptUntilItStopsCounting() {
        Incident incident = Incident.of(IncidentKind.CANCELLATION, RESERVATION, FRIDAY_22_00);

        assertThat(incident.stopsCountingAt()).isEqualTo(TWELVE_MONTHS_LATER);
        assertThat(incident.countsAt(TWELVE_MONTHS_LATER.minusNanos(1))).isTrue();
        assertThat(incident.countsAt(TWELVE_MONTHS_LATER)).isFalse();
        assertThat(incident.member()).isEmpty();
        assertThat(Incident.memberLeft(RESERVATION, "member-nurul", FRIDAY_22_00).member()).contains("member-nurul");
    }

    @Test
    @Tag("RTG-10")
    @DisplayName("Only a member who left names a member, and one that did names who.")
    void onlyAMemberWhoLeftNamesAMember() {
        assertThatThrownBy(() -> new Incident(IncidentKind.NO_SHOW, RESERVATION, "member-nurul", FRIDAY_22_00))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Incident(IncidentKind.MEMBER_LEFT, RESERVATION, null, FRIDAY_22_00))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Incident(IncidentKind.MEMBER_LEFT, RESERVATION, " ", FRIDAY_22_00))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Incident(null, RESERVATION, null, FRIDAY_22_00))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Incident(IncidentKind.NO_SHOW, null, null, FRIDAY_22_00))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Incident(IncidentKind.NO_SHOW, RESERVATION, null, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @Tag("RTG-10")
    @DisplayName("A record needs its party.")
    void aRecordNeedsItsParty() {
        assertThatThrownBy(() -> ReliabilityRecord.openFor(null)).isInstanceOf(NullPointerException.class);
    }
}
