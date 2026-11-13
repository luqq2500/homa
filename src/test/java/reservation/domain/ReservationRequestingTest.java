package reservation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static reservation.support.ReservationFixtures.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import kernel.City;
import kernel.GeoPoint;
import kernel.Money;
import kernel.Percentage;
import kernel.TimeRange;
import org.junit.jupiter.api.Test;
import reservation.domain.exception.CityNotCoveredException;
import reservation.domain.exception.InvalidDurationException;
import reservation.domain.exception.InvalidHelpersException;
import reservation.domain.exception.InvalidStartTimeException;
import reservation.domain.exception.JobsNotOfferedException;
import reservation.domain.exception.ProviderNotLiveException;
import reservation.domain.exception.ProviderNotVerifiedForJobException;
import reservation.domain.exception.TimeNotFreeException;

class ReservationRequestingTest {

    private static Reservation request(BookingDetails details) {
        return Reservation.request(customer(), terms(), VERIFIED, freeSchedule(), details, VALUES, NOW);
    }

    private static BookingDetails startingAt(Instant start) {
        return new BookingDetails(Set.of(KITCHEN), start, 3, 2);
    }

    @Test
    void aRequestKeepsWhatItWasMadeWithAndWaitsForItsPayment() {
        Reservation reservation = requested();
        ReservationState state = reservation.state();

        assertThat(reservation.id()).isNotBlank().isNotEqualTo(requested().id());
        assertThat(reservation.status()).isEqualTo(ReservationStatus.REQUESTED);
        assertThat(reservation.isPaymentSecured()).isFalse();
        assertThat(reservation.customerId()).isEqualTo(CUSTOMER);
        assertThat(reservation.providerId()).isEqualTo(PROVIDER);
        assertThat(reservation.details()).isEqualTo(DETAILS);
        assertThat(state.customerName()).isEqualTo("Aisyah binti Rahman");
        assertThat(state.address()).isEqualTo(HOME);
        assertThat(state.accessNotes()).isEqualTo("Unit 12-3, register at the guardhouse");
        assertThat(state.terms()).isEqualTo(terms());
        assertThat(state.values()).isEqualTo(VALUES);
        assertThat(state.requestedAt()).isEqualTo(NOW);
        assertThat(reservation.ending()).isEmpty();
        assertThat(reservation.settlement()).isEmpty();
    }

    @Test
    void thePriceIsClearUpfront() {
        PriceBreakdown price = requested().price();

        assertThat(price.hoursFee()).isEqualTo(Money.ofRinggit(180));
        assertThat(price.travelFee()).isEqualTo(Money.ofRinggit(10));
        assertThat(price.providerPrice()).isEqualTo(Money.ofRinggit(190));
        assertThat(price.serviceFee()).isEqualTo(Money.ofSen(2850));
        assertThat(price.total()).isEqualTo(Money.ofSen(21850));
    }

    @Test
    void aTravelFeeIncludedInThePriceIsNotChargedSeparately() {
        ProviderTerms included = new ProviderTerms(PROVIDER, true, Set.of(KITCHEN), 1, 3, new SupervisorRule(0, 0),
                Money.ofRinggit(30), false, Money.ZERO, 2, 5, Set.of(City.CYBERJAYA), CancellationTier.FIRM,
                AcceptancePolicy.REQUEST_AND_ACCEPT);

        PriceBreakdown price = PriceBreakdown.of(included, DETAILS, Percentage.of(15));

        assertThat(price.travelFee()).isEqualTo(Money.ZERO);
        assertThat(price.providerPrice()).isEqualTo(Money.ofRinggit(180));
        assertThat(price.total()).isEqualTo(Money.ofRinggit(207));
        assertThat(price.customerNoShowFee(Percentage.of(30))).isEqualTo(Money.ofRinggit(54));
    }

    @Test
    void feesAndFinesAreSharesOfTheHoursFee() {
        PriceBreakdown price = requested().price();

        assertThat(price.customerNoShowFee(Percentage.of(30))).isEqualTo(Money.ofRinggit(64));
        assertThat(price.cancellationFee(Percentage.of(10), Percentage.of(30))).isEqualTo(Money.ofRinggit(18));
        assertThat(price.fine(Percentage.of(20))).isEqualTo(Money.ofRinggit(36));
    }

    @Test
    void noCancellationFeeIsHigherThanTheCustomerNoShowFee() {
        PriceBreakdown price = requested().price();

        assertThat(price.cancellationFee(Percentage.of(50), Percentage.of(30))).isEqualTo(Money.ofRinggit(64));
    }

    @Test
    void onlyALiveProviderWhoCoversTheCustomersCityCanBeRequested() {
        ProviderTerms offline = new ProviderTerms(PROVIDER, false, Set.of(KITCHEN), 1, 3, new SupervisorRule(0, 0),
                Money.ofRinggit(30), true, Money.ofRinggit(10), 2, 5, Set.of(City.CYBERJAYA), CancellationTier.FIRM,
                AcceptancePolicy.REQUEST_AND_ACCEPT);
        Customer elsewhere = new Customer(CUSTOMER, "Aisyah",
                new Address("8 Jalan ST 1", new GeoPoint(2.81, 101.72), City.SALAK_TINGGI), "");

        assertThatThrownBy(() -> Reservation.request(customer(), offline, VERIFIED, freeSchedule(), DETAILS, VALUES, NOW))
                .isInstanceOf(ProviderNotLiveException.class);
        assertThatThrownBy(() -> Reservation.request(elsewhere, terms(), VERIFIED, freeSchedule(), DETAILS, VALUES, NOW))
                .isInstanceOf(CityNotCoveredException.class);
    }

    @Test
    void theBookedJobsAreAtLeastOneAndAllOfferedByTheProvider() {
        assertThat(request(new BookingDetails(Set.of(KITCHEN, LAUNDRY), START, 3, 2)).details().jobIds()).hasSize(2);
        assertThatThrownBy(() -> request(new BookingDetails(Set.of(), START, 3, 2)))
                .isInstanceOf(JobsNotOfferedException.class);
        assertThatThrownBy(() -> request(new BookingDetails(Set.of(KITCHEN, "job-plumbing"), START, 3, 2)))
                .isInstanceOf(JobsNotOfferedException.class);
    }

    @Test
    void startTimesAreInFifteenMinuteSteps() {
        assertThat(request(startingAt(START.plus(Duration.ofMinutes(15)))).status()).isEqualTo(ReservationStatus.REQUESTED);
        assertThatThrownBy(() -> request(startingAt(START.plus(Duration.ofMinutes(10)))))
                .isInstanceOf(InvalidStartTimeException.class);
        assertThatThrownBy(() -> request(startingAt(START.plusSeconds(1)))).isInstanceOf(InvalidStartTimeException.class);
    }

    @Test
    void theStartIsAtLeastTheMinimumNoticeFromNow() {
        Instant twoHoursOn = NOW.plus(Duration.ofHours(2));

        assertThat(request(startingAt(twoHoursOn)).details().start()).isEqualTo(twoHoursOn);
        assertThatThrownBy(() -> request(startingAt(twoHoursOn.minus(Duration.ofMinutes(15)))))
                .isInstanceOf(InvalidStartTimeException.class);
    }

    @Test
    void theStartIsNoLaterThanTheBookingHorizon() {
        Instant thirtyDaysOn = NOW.plus(Duration.ofDays(30));

        assertThat(request(startingAt(thirtyDaysOn)).details().start()).isEqualTo(thirtyDaysOn);
        assertThatThrownBy(() -> request(startingAt(thirtyDaysOn.plus(Duration.ofMinutes(15)))))
                .isInstanceOf(InvalidStartTimeException.class);
    }

    @Test
    void theDurationFitsBothHomasRangeAndTheProvidersOwnHours() {
        assertThat(request(new BookingDetails(Set.of(KITCHEN), START, 2, 2)).details().hours()).isEqualTo(2);
        assertThat(request(new BookingDetails(Set.of(KITCHEN), START, 5, 2)).details().hours()).isEqualTo(5);
        assertThatThrownBy(() -> request(new BookingDetails(Set.of(KITCHEN), START, 1, 2)))
                .isInstanceOf(InvalidDurationException.class);
        assertThatThrownBy(() -> request(new BookingDetails(Set.of(KITCHEN), START, 6, 2)))
                .isInstanceOf(InvalidDurationException.class);
    }

    @Test
    void homasRangeBoundsTheDurationEvenWhenTheProvidersIsWider() {
        ProviderTerms wide = new ProviderTerms(PROVIDER, true, Set.of(KITCHEN), 1, 3, new SupervisorRule(0, 0),
                Money.ofRinggit(30), true, Money.ofRinggit(10), 1, 8, Set.of(City.CYBERJAYA), CancellationTier.FIRM,
                AcceptancePolicy.REQUEST_AND_ACCEPT);
        HomaValues narrow = new HomaValues(2, Percentage.of(15), 2, 4, Duration.ofHours(2), Duration.ofDays(30),
                Duration.ofHours(6), Duration.ofMinutes(15), Duration.ofMinutes(15), 300, Duration.ofMinutes(15),
                Percentage.of(30), Duration.ofHours(1), Duration.ofHours(24), Duration.ofHours(1),
                Duration.ofMinutes(30), Duration.ofHours(24), Duration.ofHours(24), Duration.ofHours(24),
                Percentage.of(10), Percentage.of(20), Percentage.of(20), Percentage.of(30));

        new BookingDetails(Set.of(KITCHEN), START, 2, 2).checkBookable(wide, narrow, NOW);
        new BookingDetails(Set.of(KITCHEN), START, 4, 2).checkBookable(wide, narrow, NOW);
        assertThatThrownBy(() -> new BookingDetails(Set.of(KITCHEN), START, 1, 2).checkBookable(wide, narrow, NOW))
                .isInstanceOf(InvalidDurationException.class);
        assertThatThrownBy(() -> new BookingDetails(Set.of(KITCHEN), START, 5, 2).checkBookable(wide, narrow, NOW))
                .isInstanceOf(InvalidDurationException.class);
    }

    @Test
    void theHelpersAreWithinTheProvidersRange() {
        assertThat(request(new BookingDetails(Set.of(KITCHEN), START, 3, 1)).details().helpers()).isEqualTo(1);
        assertThat(request(new BookingDetails(Set.of(KITCHEN), START, 3, 3)).details().helpers()).isEqualTo(3);
        assertThatThrownBy(() -> request(new BookingDetails(Set.of(KITCHEN), START, 3, 0)))
                .isInstanceOf(InvalidHelpersException.class);
        assertThatThrownBy(() -> request(new BookingDetails(Set.of(KITCHEN), START, 3, 4)))
                .isInstanceOf(InvalidHelpersException.class);
    }

    @Test
    void aBusinessIsRequestedOnlyForAJobThatEndsByItsVerifiedUntilDate() {
        ProviderStanding untilTheEnd = new ProviderStanding(PROVIDER, END, false);
        ProviderStanding lapsesDuringTheJob = new ProviderStanding(PROVIDER, END.minusSeconds(1), false);
        ProviderStanding flagged = new ProviderStanding(PROVIDER, END.plusSeconds(3600), true);

        assertThat(untilTheEnd.isVerifiedFor(END)).isTrue();
        assertThatThrownBy(() -> Reservation.request(customer(), terms(), lapsesDuringTheJob, freeSchedule(), DETAILS, VALUES, NOW))
                .isInstanceOf(ProviderNotVerifiedForJobException.class);
        assertThatThrownBy(() -> Reservation.request(customer(), terms(), flagged, freeSchedule(), DETAILS, VALUES, NOW))
                .isInstanceOf(ProviderNotVerifiedForJobException.class);
        assertThatThrownBy(() -> Reservation.request(customer(), terms(), ProviderStanding.unverified(PROVIDER),
                freeSchedule(), DETAILS, VALUES, NOW)).isInstanceOf(ProviderNotVerifiedForJobException.class);
    }

    @Test
    void aRequestIsOnlyMadeForATimeTheProviderIsFree() {
        ProviderSchedule busy = schedule(List.of(TimeRange.of(START.plus(Duration.ofHours(1)), END)));
        ProviderSchedule closed = new ProviderSchedule(List.of(TimeRange.of(START, END.minusSeconds(900))),
                Duration.ZERO, List.of());

        assertThatThrownBy(() -> Reservation.request(customer(), terms(), VERIFIED, busy, DETAILS, VALUES, NOW))
                .isInstanceOf(TimeNotFreeException.class);
        assertThatThrownBy(() -> Reservation.request(customer(), terms(), VERIFIED, closed, DETAILS, VALUES, NOW))
                .isInstanceOf(TimeNotFreeException.class);
    }

    @Test
    void aNewBookingCannotStartOrEndWithinTheBufferAroundAnotherJob() {
        TimeRange job = TimeRange.of(START, END);
        ProviderSchedule thirtyMinuteBuffer = schedule(List.of(job));

        assertThat(thirtyMinuteBuffer.clashes(TimeRange.of(END.plus(Duration.ofMinutes(30)), END.plus(Duration.ofHours(2))))).isFalse();
        assertThat(thirtyMinuteBuffer.clashes(TimeRange.of(END.plus(Duration.ofMinutes(15)), END.plus(Duration.ofHours(2))))).isTrue();
        assertThat(thirtyMinuteBuffer.clashes(TimeRange.of(START.minus(Duration.ofHours(2)), START.minus(Duration.ofMinutes(30))))).isFalse();
        assertThat(thirtyMinuteBuffer.clashes(TimeRange.of(START.minus(Duration.ofHours(2)), START.minus(Duration.ofMinutes(15))))).isTrue();
        assertThat(thirtyMinuteBuffer.isFreeFor(TimeRange.of(END.plus(Duration.ofMinutes(30)), END.plus(Duration.ofHours(2))))).isTrue();
        assertThat(thirtyMinuteBuffer.isFreeFor(job)).isFalse();
    }

    @Test
    void aSupervisorIsIncludedWhenTheBookingReachesEitherThreshold() {
        SupervisorRule rule = new SupervisorRule(3, 5);

        assertThat(rule.includesSupervisorFor(2, 4)).isFalse();
        assertThat(rule.includesSupervisorFor(3, 2)).isTrue();
        assertThat(rule.includesSupervisorFor(1, 5)).isTrue();
        assertThat(new SupervisorRule(0, 0).includesSupervisorFor(9, 6)).isFalse();
        assertThat(new SupervisorRule(0, 4).includesSupervisorFor(1, 4)).isTrue();
        assertThat(requested().supervisorIncluded()).isFalse();
        assertThat(request(new BookingDetails(Set.of(KITCHEN), START, 3, 3)).supervisorIncluded()).isTrue();
    }

    @Test
    void flexibleIsAlwaysFreeAndTheOtherTiersTakeTheirShare() {
        assertThat(VALUES.cancellationFeeOf(CancellationTier.FLEXIBLE)).isEqualTo(Percentage.ZERO);
        assertThat(VALUES.cancellationFeeOf(CancellationTier.STANDARD)).isEqualTo(Percentage.of(10));
        assertThat(VALUES.cancellationFeeOf(CancellationTier.FIRM)).isEqualTo(Percentage.of(20));
    }

    @Test
    void aStoredReservationIsRestoredExactly() {
        Reservation reservation = completionSubmitted();
        reservation.fileReport(new Report(ReportingParty.CUSTOMER, ReportType.FINISHED_WORK, "Oven not cleaned",
                List.of("evidence/oven.jpg"), END.plusSeconds(60)));

        ReservationState state = reservation.state();

        assertThat(Reservation.restore(state).state()).isEqualTo(state);
        assertThat(state.status()).isEqualTo(ReservationStatus.DISPUTED);
        assertThat(state.faceChecks()).containsKey("helper-1");
    }
}
