package reservation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static reservation.support.ReservationFixtures.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import kernel.Money;
import org.junit.jupiter.api.Test;
import reservation.domain.exception.AccessNotesTooLongException;
import reservation.domain.exception.CompletionAlreadySubmittedException;
import reservation.domain.exception.DecisionOutOfRangeException;
import reservation.domain.exception.EnquiryClosedException;
import reservation.domain.exception.EvidenceMissingException;
import reservation.domain.exception.ReasonMissingException;
import reservation.domain.exception.ReportAlreadyFiledException;
import reservation.domain.exception.ReportWindowClosedException;
import reservation.domain.exception.ReservationStatusException;

class ReservationCompletionAndDisputeTest {

    private static final Instant WINDOW_ENDS = END.plus(Duration.ofHours(24));

    private static Report report(ReportingParty party, Instant filedAt) {
        return new Report(party, ReportType.DURING_THE_JOB, "A helper was missing", List.of("evidence/door.jpg"), filedAt);
    }

    private static Reservation disputedByCustomer() {
        Reservation reservation = checkedIn();
        reservation.fileReport(report(ReportingParty.CUSTOMER, START.plusSeconds(600)));
        return reservation;
    }

    @Test
    void theProviderMarksTheJobCompletedWithEvidenceAndTheStatusDoesNotChange() {
        Reservation reservation = checkedIn();

        reservation.submitCompletion(List.of("evidence/kitchen.jpg", "evidence/kitchen.mp4"), END.minusSeconds(600));

        assertThat(reservation.status()).isEqualTo(ReservationStatus.RESERVED);
        assertThat(reservation.state().completion())
                .isEqualTo(new Completion(List.of("evidence/kitchen.jpg", "evidence/kitchen.mp4"), END.minusSeconds(600)));
    }

    @Test
    void completionNeedsEvidenceACheckInAndIsSubmittedOnce() {
        assertThatThrownBy(() -> checkedIn().submitCompletion(List.of(), END)).isInstanceOf(EvidenceMissingException.class);
        assertThatThrownBy(() -> checkedIn().submitCompletion(null, END)).isInstanceOf(EvidenceMissingException.class);
        assertThatThrownBy(() -> teamNamed().submitCompletion(List.of("e.jpg"), END)).isInstanceOf(ReservationStatusException.class);
        assertThatThrownBy(() -> completionSubmitted().submitCompletion(List.of("e.jpg"), END))
                .isInstanceOf(CompletionAlreadySubmittedException.class);
    }

    @Test
    void theDisputeWindowEndsTwentyFourHoursAfterTheLaterOfTheBookedEndAndCompletion() {
        Reservation early = checkedIn();
        early.submitCompletion(List.of("e.jpg"), END.minusSeconds(600));
        Reservation late = checkedIn();
        late.submitCompletion(List.of("e.jpg"), END.plus(Duration.ofHours(2)));

        assertThat(early.disputeWindowEndsAt()).contains(WINDOW_ENDS);
        assertThat(late.disputeWindowEndsAt()).contains(WINDOW_ENDS.plus(Duration.ofHours(2)));
        assertThat(checkedIn().disputeWindowEndsAt()).isEmpty();
    }

    @Test
    void whenTheDisputeWindowEndsWithNoReportTheReservationIsCompletedAndThePaymentReleased() {
        Reservation reservation = completionSubmitted();

        assertThat(reservation.completeAfterDisputeWindow(WINDOW_ENDS.minusSeconds(1))).isFalse();
        assertThat(reservation.completeAfterDisputeWindow(WINDOW_ENDS)).isTrue();

        assertThat(reservation.status()).isEqualTo(ReservationStatus.COMPLETED);
        assertThat(reservation.ending()).contains(Ending.DISPUTE_WINDOW_ENDED);
        assertThat(reservation.settlement()).contains(new Settlement(Money.ofRinggit(190), PayoutKind.JOB));
        assertThat(reservation.completeAfterDisputeWindow(WINDOW_ENDS.plusSeconds(1))).isFalse();
    }

    @Test
    void thereIsNoAutomaticCompletionWithoutTheProvidersSubmission() {
        Reservation reservation = checkedIn();

        assertThat(reservation.completeAfterDisputeWindow(END.plus(Duration.ofDays(30)))).isFalse();
        assertThat(reservation.status()).isEqualTo(ReservationStatus.RESERVED);
    }

    @Test
    void homaRemindsTheProviderOnceToSubmitCompletion() {
        Reservation reservation = checkedIn();
        Instant reminderTime = END.plus(Duration.ofMinutes(30));

        assertThat(reservation.remindCompletion(reminderTime.minusSeconds(1))).isFalse();
        assertThat(reservation.remindCompletion(reminderTime)).isTrue();
        assertThat(reservation.remindCompletion(reminderTime.plusSeconds(1))).isFalse();
        assertThat(completionSubmitted().remindCompletion(reminderTime)).isFalse();
        assertThat(teamNamed().remindCompletion(reminderTime)).isFalse();
    }

    @Test
    void missingTheCompletionDeadlineMovesTheReservationToDisputed() {
        Reservation reservation = checkedIn();
        Instant deadline = END.plus(Duration.ofHours(24));

        assertThat(reservation.missCompletionDeadline(deadline.minusSeconds(1))).isFalse();
        assertThat(reservation.missCompletionDeadline(deadline)).isTrue();

        assertThat(reservation.status()).isEqualTo(ReservationStatus.DISPUTED);
        assertThat(reservation.disputeCause()).contains(DisputeCause.MISSED_COMPLETION_DEADLINE);
        assertThat(reservation.missCompletionDeadline(deadline.plusSeconds(1))).isFalse();
        assertThat(completionSubmitted().missCompletionDeadline(deadline)).isFalse();
        assertThat(teamNamed().missCompletionDeadline(deadline)).isFalse();
    }

    @Test
    void theProviderCanStillSubmitCompletionWhileDisputedOverTheJobAndItStaysDisputed() {
        Reservation reservation = checkedIn();
        reservation.missCompletionDeadline(END.plus(Duration.ofHours(24)));
        Instant submitted = END.plus(Duration.ofHours(30));

        reservation.submitCompletion(List.of("e.jpg"), submitted);

        assertThat(reservation.status()).isEqualTo(ReservationStatus.DISPUTED);
        assertThat(reservation.disputeWindowEndsAt()).contains(submitted.plus(Duration.ofHours(24)));
        reservation.fileReport(report(ReportingParty.CUSTOMER, submitted.plus(Duration.ofHours(23))));
        assertThat(reservation.state().customerReport()).isNotNull();
        assertThat(reservation.disputeCause()).contains(DisputeCause.MISSED_COMPLETION_DEADLINE);
    }

    @Test
    void aReportMovesTheReservationToDisputed() {
        Reservation reservation = checkedIn();
        Report filed = report(ReportingParty.CUSTOMER, START);

        reservation.fileReport(filed);

        assertThat(reservation.status()).isEqualTo(ReservationStatus.DISPUTED);
        assertThat(reservation.disputeCause()).contains(DisputeCause.REPORT);
        assertThat(reservation.state().customerReport()).isEqualTo(filed);
        assertThat(reservation.completeAfterDisputeWindow(WINDOW_ENDS)).isFalse();
    }

    @Test
    void reportsOpenAtTheBookedStartAndCloseAtTheMomentTheDisputeWindowEnds() {
        assertThatThrownBy(() -> teamNamed().fileReport(report(ReportingParty.CUSTOMER, START.minusSeconds(1))))
                .isInstanceOf(ReportWindowClosedException.class);

        Reservation justInTime = completionSubmitted();
        justInTime.fileReport(report(ReportingParty.PROVIDER, WINDOW_ENDS.minusSeconds(1)));
        assertThat(justInTime.state().providerReport()).isNotNull();

        assertThatThrownBy(() -> completionSubmitted().fileReport(report(ReportingParty.CUSTOMER, WINDOW_ENDS)))
                .isInstanceOf(ReportWindowClosedException.class);
    }

    @Test
    void eachPartyFilesOneReportAndTheOtherCanStillFileTheirsWhileDisputed() {
        Reservation reservation = disputedByCustomer();

        assertThatThrownBy(() -> reservation.fileReport(report(ReportingParty.CUSTOMER, START.plusSeconds(700))))
                .isInstanceOf(ReportAlreadyFiledException.class);
        reservation.fileReport(report(ReportingParty.PROVIDER, START.plusSeconds(800)));

        assertThat(reservation.state().providerReport().filedAt()).isEqualTo(START.plusSeconds(800));
        assertThat(reservation.state().customerReport().filedAt()).isEqualTo(START.plusSeconds(600));
        assertThatThrownBy(() -> reservation.fileReport(report(ReportingParty.PROVIDER, START.plusSeconds(900))))
                .isInstanceOf(ReportAlreadyFiledException.class);
    }

    @Test
    void aReportHasAReasonAndOnlyAnAcceptedReservationTakesOne() {
        assertThatThrownBy(() -> new Report(ReportingParty.CUSTOMER, ReportType.FINISHED_WORK, " ", List.of(), START))
                .isInstanceOf(ReasonMissingException.class);
        assertThatThrownBy(() -> new Report(ReportingParty.CUSTOMER, ReportType.FINISHED_WORK, null, List.of(), START))
                .isInstanceOf(ReasonMissingException.class);
        assertThatThrownBy(() -> secured(terms()).fileReport(report(ReportingParty.CUSTOMER, START)))
                .isInstanceOf(ReservationStatusException.class);
    }

    @Test
    void aChallengedNoShowTakesNoReports() {
        Reservation reservation = checkedIn();
        reservation.reportCustomerNoShow(START.plus(Duration.ofMinutes(20)));
        reservation.challengeNoShow(START.plus(Duration.ofMinutes(30)));

        assertThatThrownBy(() -> reservation.fileReport(report(ReportingParty.CUSTOMER, START.plus(Duration.ofMinutes(40)))))
                .isInstanceOf(ReservationStatusException.class);
    }

    @Test
    void homaDecidesAJobDisputeWithAnyAmountUpToTheProviderPriceAndItBecomesCompleted() {
        Reservation release = disputedByCustomer();
        Reservation partial = disputedByCustomer();
        Reservation fullRefund = disputedByCustomer();
        assertThat(release.atStake()).isEqualTo(Money.ofRinggit(190));

        release.decide(Money.ofRinggit(190));
        partial.decide(Money.ofRinggit(95));
        fullRefund.decide(Money.ZERO);

        assertThat(release.status()).isEqualTo(ReservationStatus.COMPLETED);
        assertThat(release.ending()).contains(Ending.DECIDED_BY_HOMA);
        assertThat(release.settlement()).contains(new Settlement(Money.ofRinggit(190), PayoutKind.JOB));
        assertThat(partial.settlement()).contains(new Settlement(Money.ofRinggit(95), PayoutKind.JOB));
        assertThat(fullRefund.status()).isEqualTo(ReservationStatus.COMPLETED);
        assertThat(fullRefund.settlement()).contains(new Settlement(Money.ZERO, PayoutKind.JOB));
    }

    @Test
    void aDecisionCannotGiveTheProviderMoreThanWasAtStake() {
        Reservation reservation = disputedByCustomer();

        assertThatThrownBy(() -> reservation.decide(Money.ofSen(19001))).isInstanceOf(DecisionOutOfRangeException.class);
        assertThat(reservation.status()).isEqualTo(ReservationStatus.DISPUTED);
        assertThatThrownBy(() -> checkedIn().decide(Money.ZERO)).isInstanceOf(ReservationStatusException.class);
    }

    @Test
    void aChallengedNoShowEndsCancelledWhateverHomaDecides() {
        Reservation reservation = checkedIn();
        reservation.reportCustomerNoShow(START.plus(Duration.ofMinutes(20)));
        reservation.challengeNoShow(START.plus(Duration.ofMinutes(30)));

        assertThatThrownBy(() -> reservation.decide(Money.ofSen(6401))).isInstanceOf(DecisionOutOfRangeException.class);
        reservation.decide(Money.ofRinggit(32));

        assertThat(reservation.status()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(reservation.ending()).contains(Ending.DECIDED_BY_HOMA);
        assertThat(reservation.settlement()).contains(new Settlement(Money.ofRinggit(32), PayoutKind.CUSTOMER_NO_SHOW_FEE));
        assertThat(reservation.state().customerNoShowFeeReleased()).isTrue();
    }

    @Test
    void aSeriousFlagMovesAnAcceptedReservationWithoutCompletionToDisputed() {
        Reservation upcoming = reserved();
        Reservation underWay = checkedIn();
        Reservation alreadyDisputed = disputedByCustomer();

        assertThat(upcoming.disputeForSeriousFlag()).isTrue();
        assertThat(underWay.disputeForSeriousFlag()).isTrue();
        assertThat(alreadyDisputed.disputeForSeriousFlag()).isTrue();

        assertThat(upcoming.status()).isEqualTo(ReservationStatus.DISPUTED);
        assertThat(upcoming.disputeCause()).contains(DisputeCause.SERIOUS_FLAG);
        assertThat(upcoming.isDisputedForSeriousFlag()).isTrue();
        assertThat(alreadyDisputed.state().disputeCauseBeforeFlag()).isEqualTo(DisputeCause.REPORT);
        assertThat(alreadyDisputed.state().customerReport()).isNotNull();
        assertThat(upcoming.disputeForSeriousFlag()).isFalse();
    }

    @Test
    void aFlagLeavesRequestsAndReservationsWithCompletionSubmittedAsTheyAre() {
        Reservation submitted = completionSubmitted();
        Reservation request = secured(terms());

        assertThat(submitted.disputeForSeriousFlag()).isFalse();
        assertThat(request.disputeForSeriousFlag()).isFalse();
        assertThat(submitted.status()).isEqualTo(ReservationStatus.RESERVED);
        assertThat(submitted.isDisputedForSeriousFlag()).isFalse();
        assertThat(disputedByCustomer().isDisputedForSeriousFlag()).isFalse();
    }

    @Test
    void aFlaggedReservationIsNormallyCancelledWithAFullRefundAndNoFine() {
        Reservation reservation = reserved();
        reservation.disputeForSeriousFlag();

        assertThatThrownBy(() -> reservation.decide(Money.ZERO)).isInstanceOf(ReservationStatusException.class);
        reservation.cancelForSeriousFlag();

        assertThat(reservation.status()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(reservation.ending()).contains(Ending.CANCELLED_FOR_SERIOUS_FLAG);
        assertThat(reservation.settlement()).contains(Settlement.fullRefund());
        assertThat(reservation.fine()).isEmpty();
        assertThatThrownBy(() -> reserved().cancelForSeriousFlag()).isInstanceOf(ReservationStatusException.class);
    }

    @Test
    void onceTheFlagIsClearedBeforeTheStartTheReservationGoesAheadAsBooked() {
        Reservation reservation = reserved();
        reservation.disputeForSeriousFlag();

        reservation.restoreAfterFlagCleared(START.minusSeconds(1));

        assertThat(reservation.status()).isEqualTo(ReservationStatus.RESERVED);
        assertThat(reservation.disputeCause()).isEmpty();
        assertThat(reservation.state().disputeCauseBeforeFlag()).isNull();
    }

    @Test
    void aRestoredReservationKeepsTheDisputeItHadBeforeTheFlag() {
        Reservation reservation = teamNamed();
        reservation.checkIn("helper-1", FaceCheckResult.PASSED, true, START.minus(Duration.ofMinutes(10)));
        reservation.disputeForSeriousFlag();
        Reservation disputed = Reservation.restore(withCauseBeforeFlag(reservation.state(), DisputeCause.REPORT));

        disputed.restoreAfterFlagCleared(START.minus(Duration.ofMinutes(5)));

        assertThat(disputed.status()).isEqualTo(ReservationStatus.DISPUTED);
        assertThat(disputed.disputeCause()).contains(DisputeCause.REPORT);
    }

    private static ReservationState withCauseBeforeFlag(ReservationState s, DisputeCause cause) {
        return new ReservationState(s.id(), s.customerId(), s.customerName(), s.address(), s.accessNotes(), s.terms(),
                s.values(), s.details(), s.price(), s.status(), s.ending(), s.requestedAt(), s.paymentSecured(),
                s.acceptedAt(), s.jobTeam(), s.teamReminded(), s.checkedInAt(), s.faceChecks(), s.completion(),
                s.completionReminded(), s.customerReport(), s.providerReport(), s.disputeCause(), cause,
                s.customerNoShowReportedAt(), s.customerNoShowFeeReleased(), s.settlement(), s.fine());
    }

    @Test
    void aFlaggedReservationCannotBeRestoredOnceTheBookedStartHasCome() {
        Reservation reservation = reserved();
        reservation.disputeForSeriousFlag();

        assertThatThrownBy(() -> reservation.restoreAfterFlagCleared(START)).isInstanceOf(ReservationStatusException.class);
        assertThatThrownBy(() -> reserved().restoreAfterFlagCleared(NOW)).isInstanceOf(ReservationStatusException.class);
        assertThat(reservation.isDisputedForSeriousFlag()).isTrue();
    }

    @Test
    void ifNobodyHasDecidedByTheBookedStartHomaCancelsWithAFullRefund() {
        Reservation reservation = reserved();
        reservation.disputeForSeriousFlag();

        assertThat(reservation.cancelUndecidedFlagAtStart(START.minusSeconds(1))).isFalse();
        assertThat(reservation.cancelUndecidedFlagAtStart(START)).isTrue();

        assertThat(reservation.status()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(reservation.settlement()).contains(Settlement.fullRefund());
        assertThat(reserved().cancelUndecidedFlagAtStart(START)).isFalse();
    }

    @Test
    void theCustomerChangesAccessNotesUntilTheJobIsCompleted() {
        Reservation reservation = checkedIn();

        reservation.changeAccessNotes("a".repeat(500));
        assertThat(reservation.state().accessNotes()).hasSize(500);
        reservation.changeAccessNotes(null);
        assertThat(reservation.state().accessNotes()).isEmpty();

        assertThatThrownBy(() -> reservation.changeAccessNotes("a".repeat(501))).isInstanceOf(AccessNotesTooLongException.class);
        assertThatThrownBy(() -> completionSubmitted().changeAccessNotes("Gate B")).isInstanceOf(ReservationStatusException.class);
        Reservation cancelled = reserved();
        cancelled.cancelByProvider(NOW);
        assertThatThrownBy(() -> cancelled.changeAccessNotes("Gate B")).isInstanceOf(ReservationStatusException.class);
        requested().changeAccessNotes("Gate B");
        disputedByCustomer().changeAccessNotes("Gate B");
    }

    @Test
    void anEnquiryKeepsTheChosenDetailsAndBecomesAReservationOnce() {
        Enquiry enquiry = Enquiry.start(CUSTOMER, PROVIDER, DETAILS);

        assertThat(enquiry.id()).isNotBlank().isNotEqualTo(Enquiry.start(CUSTOMER, PROVIDER, DETAILS).id());
        assertThat(enquiry.customerId()).isEqualTo(CUSTOMER);
        assertThat(enquiry.providerId()).isEqualTo(PROVIDER);
        assertThat(enquiry.details()).isEqualTo(DETAILS);
        assertThat(enquiry.reservationId()).isNull();
        assertThat(enquiry.isClosed()).isFalse();

        enquiry.becomeReservation("reservation-1");
        assertThat(enquiry.reservationId()).isEqualTo("reservation-1");
        assertThatThrownBy(() -> enquiry.becomeReservation("reservation-2")).isInstanceOf(EnquiryClosedException.class);
        assertThat(enquiry.closeIfDatePassed(START)).isFalse();
    }

    @Test
    void anEnquiryThatIsNeverRequestedClosesWhenItsDatePasses() {
        Enquiry enquiry = Enquiry.start(CUSTOMER, PROVIDER, DETAILS);

        assertThat(enquiry.closeIfDatePassed(START.minusSeconds(1))).isFalse();
        assertThat(enquiry.closeIfDatePassed(START)).isTrue();

        assertThat(enquiry.isClosed()).isTrue();
        assertThat(enquiry.closeIfDatePassed(START.plusSeconds(1))).isFalse();
        assertThatThrownBy(() -> enquiry.becomeReservation("reservation-1")).isInstanceOf(EnquiryClosedException.class);
    }

    @Test
    void bookAgainCopiesTheProviderJobsDurationAndHelpersAtANewDateAndTime() {
        Instant newStart = START.plus(Duration.ofDays(7));

        Enquiry again = Enquiry.bookAgain(CUSTOMER, PROVIDER, DETAILS, newStart);

        assertThat(again.providerId()).isEqualTo(PROVIDER);
        assertThat(again.details()).isEqualTo(new BookingDetails(Set.of(KITCHEN), newStart, 3, 2));
        assertThat(new Enquiry("e-1", CUSTOMER, PROVIDER, DETAILS, "r-1", true).isClosed()).isTrue();
    }
}
