package reservation.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static reservation.support.ReservationFixtures.*;

import ie.events.BusinessSeriouslyFlaggedIE;
import ie.events.CaseDecidedIE;
import ie.events.CompletionDeadlineMissedIE;
import ie.events.CompletionSubmittedIE;
import ie.events.FlaggedReservationDecidedIE;
import ie.events.ReportFiledIE;
import ie.events.ReservationCompletedIE;
import ie.events.ReservationDisputedForFlagIE;
import ie.events.ReservationEndedIE;
import ie.events.ReservationRestoredIE;
import ie.events.ReservationSettledIE;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import kernel.Money;
import org.junit.jupiter.api.Test;
import reservation.domain.ReservationStatus;
import reservation.support.ReservationWorld;
import reservation.usecase.dto.ChangeAccessNotesRequest;
import reservation.usecase.dto.FileCustomerReportRequest;
import reservation.usecase.dto.FileCustomerReportResponse;
import reservation.usecase.dto.FileProviderReportRequest;
import reservation.usecase.dto.SubmitCompletionRequest;
import reservation.usecase.dto.SubmitCompletionResponse;
import reservation.usecase.dto.ViewCustomerReservationRequest;
import reservation.usecase.dto.ViewCustomerReservationResponse;
import reservation.usecase.dto.ViewProviderReservationRequest;
import reservation.usecase.dto.ViewProviderReservationResponse;
import reservation.usecase.exception.ReservationNotFoundException;
import reservation.usecase.exception.UnknownChoiceException;

class CompletionAndDisputeUsecasesTest {

    private final ReservationWorld world = new ReservationWorld();
    private final CustomerFileReport customerReport =
            new CustomerFileReport(world.reservations, world.publisher, world.clock);

    @Test
    void submittedCompletionTellsTheCustomerAndStartsTheLastStretchOfTheDisputeWindow() {
        String id = world.checkedIn();
        world.clock.set(END);

        SubmitCompletionResponse response = new ProviderSubmitCompletion(world.reservations, world.publisher,
                world.clock).submit(new SubmitCompletionRequest(PROVIDER, id, List.of("evidence/kitchen.jpg")));

        assertThat(response).isEqualTo(new SubmitCompletionResponse(id, END.plus(Duration.ofHours(24))));
        assertThat(world.reservations.timesSaved(id)).isEqualTo(6);
        assertThat(world.publisher.published(CompletionSubmittedIE.class)).containsExactly(new CompletionSubmittedIE(id,
                CUSTOMER, PROVIDER, List.of("evidence/kitchen.jpg"), END.plus(Duration.ofHours(24)), END));
    }

    @Test
    void afterTheDisputeWindowTheReservationIsCompletedAndThePaymentReleased() {
        String id = world.completionSubmitted();
        SystemCompleteAfterDisputeWindow usecase =
                new SystemCompleteAfterDisputeWindow(world.reservations, world.publisher, world.clock);
        assertThat(usecase.completeDue().ids()).isEmpty();
        Instant windowEnds = END.plus(Duration.ofHours(24));
        world.clock.set(windowEnds);

        assertThat(usecase.completeDue().ids()).containsExactly(id);

        assertThat(world.reservation(id).status()).isEqualTo(ReservationStatus.COMPLETED);
        assertThat(world.reservations.timesSaved(id)).isEqualTo(7);
        assertThat(world.publisher.published(ReservationCompletedIE.class)).containsExactly(
                new ReservationCompletedIE(id, CUSTOMER, PROVIDER, Set.of("helper-1", "helper-2"), true, windowEnds));
        assertThat(world.publisher.published(ReservationSettledIE.class))
                .containsExactly(new ReservationSettledIE(id, PROVIDER, Money.ofRinggit(190), "JOB", windowEnds));
        assertThat(world.publisher.published(ReservationEndedIE.class)).isEmpty();
    }

    @Test
    void aFiledReportGoesToHomaWithWhatIsAtStake() {
        String id = world.checkedIn();

        FileCustomerReportResponse response = customerReport.file(new FileCustomerReportRequest(CUSTOMER, id,
                "SAFETY_DAMAGE_OR_LOSS", "A vase was broken", List.of("evidence/vase.jpg")));

        assertThat(response).isEqualTo(new FileCustomerReportResponse(id, "DISPUTED"));
        assertThat(world.reservations.timesSaved(id)).isEqualTo(6);
        assertThat(world.publisher.published(ReportFiledIE.class)).containsExactly(new ReportFiledIE(id, CUSTOMER,
                PROVIDER, "CUSTOMER", "SAFETY_DAMAGE_OR_LOSS", "A vase was broken", List.of("evidence/vase.jpg"),
                Money.ofRinggit(190), START));
    }

    @Test
    void theProviderFilesTheirOwnReportAndOnlyKnownTypesAreTaken() {
        String id = world.checkedIn();

        String status = new ProviderFileReport(world.reservations, world.publisher, world.clock)
                .file(new FileProviderReportRequest(PROVIDER, id, "DURING_THE_JOB", "Nobody home", List.of())).status();

        assertThat(status).isEqualTo("DISPUTED");
        assertThat(world.publisher.published(ReportFiledIE.class)).singleElement()
                .satisfies(filed -> assertThat(filed.party()).isEqualTo("PROVIDER"));
        assertThatThrownBy(() -> customerReport.file(new FileCustomerReportRequest(CUSTOMER, id, "RUDENESS", "x", List.of())))
                .isInstanceOf(UnknownChoiceException.class);
        assertThatThrownBy(() -> customerReport.file(new FileCustomerReportRequest(CUSTOMER, id, null, "x", List.of())))
                .isInstanceOf(UnknownChoiceException.class);
        assertThatThrownBy(() -> customerReport.file(new FileCustomerReportRequest("customer-2", id, "FINISHED_WORK", "x", List.of())))
                .isInstanceOf(ReservationNotFoundException.class);
    }

    @Test
    void aMissedCompletionDeadlineGoesToHomasReview() {
        String id = world.checkedIn();
        SystemDisputeMissedCompletions usecase =
                new SystemDisputeMissedCompletions(world.reservations, world.publisher, world.clock);
        Instant deadline = END.plus(Duration.ofHours(24));
        world.clock.set(deadline);

        assertThat(usecase.disputeDue().ids()).containsExactly(id);
        assertThat(usecase.disputeDue().ids()).isEmpty();

        assertThat(world.reservation(id).status()).isEqualTo(ReservationStatus.DISPUTED);
        assertThat(world.reservations.timesSaved(id)).isEqualTo(6);
        assertThat(world.publisher.published(CompletionDeadlineMissedIE.class)).containsExactly(
                new CompletionDeadlineMissedIE(id, CUSTOMER, PROVIDER, Money.ofRinggit(190), deadline));
    }

    @Test
    void homasDecisionCompletesTheReservationAndSplitsTheMoney() {
        String id = world.checkedIn();
        customerReport.file(new FileCustomerReportRequest(CUSTOMER, id, "FINISHED_WORK", "Oven not cleaned", List.of()));
        HandleCaseDecidedIE handler = new HandleCaseDecidedIE(world.reservations, world.publisher, world.clock);

        handler.handle(new CaseDecidedIE(id, Money.ZERO, "admin-1", "Work not done", START));
        handler.handle(new CaseDecidedIE("unknown", Money.ZERO, "admin-1", "x", START));

        assertThat(world.reservation(id).status()).isEqualTo(ReservationStatus.COMPLETED);
        assertThat(world.reservations.timesSaved(id)).isEqualTo(7);
        assertThat(world.publisher.published(ReservationCompletedIE.class)).containsExactly(
                new ReservationCompletedIE(id, CUSTOMER, PROVIDER, Set.of("helper-1", "helper-2"), false, START));
        assertThat(world.publisher.published(ReservationSettledIE.class))
                .containsExactly(new ReservationSettledIE(id, PROVIDER, Money.ZERO, "JOB", START));
    }

    @Test
    void aSeriousFlagPutsEveryAcceptedJobWithoutCompletionOnHold() {
        String upcoming = world.reserved();
        String request = world.secured(START.plus(Duration.ofDays(1)));
        HandleBusinessSeriouslyFlaggedIE handler = new HandleBusinessSeriouslyFlaggedIE(world.standings,
                world.reservations, world.publisher, world.clock);

        handler.handle(new BusinessSeriouslyFlaggedIE(PROVIDER, NOW));

        assertThat(world.standings.find(PROVIDER).orElseThrow().seriouslyFlagged()).isTrue();
        assertThat(world.standings.find(PROVIDER).orElseThrow().verifiedUntil()).isEqualTo(VERIFIED.verifiedUntil());
        assertThat(world.reservation(upcoming).isDisputedForSeriousFlag()).isTrue();
        assertThat(world.reservations.timesSaved(upcoming)).isEqualTo(4);
        assertThat(world.reservations.timesSaved(request)).isEqualTo(2);
        assertThat(world.publisher.published(ReservationDisputedForFlagIE.class))
                .containsExactly(new ReservationDisputedForFlagIE(upcoming, CUSTOMER, PROVIDER, START, NOW));
    }

    @Test
    void anAdminCancelsOrRestoresAFlaggedReservation() {
        String cancelled = world.reserved();
        world.clock.set(NOW.plusSeconds(60));
        String restored = world.secured(START.plus(Duration.ofDays(1)));
        new ProviderAcceptRequest(world.reservations, world.openTimes, world.standings, world.publisher, world.clock)
                .accept(new reservation.usecase.dto.AcceptRequestRequest(PROVIDER, restored));
        new HandleBusinessSeriouslyFlaggedIE(world.standings, world.reservations, world.publisher, world.clock)
                .handle(new BusinessSeriouslyFlaggedIE(PROVIDER, NOW));
        HandleFlaggedReservationDecidedIE handler =
                new HandleFlaggedReservationDecidedIE(world.reservations, world.publisher, world.clock);
        Instant now = world.clock.instant();

        handler.handle(new FlaggedReservationDecidedIE(cancelled, false, "admin-1", "Struck off", now));
        handler.handle(new FlaggedReservationDecidedIE(restored, true, "admin-1", "Flag cleared", now));
        handler.handle(new FlaggedReservationDecidedIE("unknown", true, "admin-1", "x", now));

        assertThat(world.reservation(cancelled).status()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(world.reservation(restored).status()).isEqualTo(ReservationStatus.RESERVED);
        assertThat(world.reservations.timesSaved(cancelled)).isEqualTo(5);
        assertThat(world.reservations.timesSaved(restored)).isEqualTo(5);
        assertThat(world.publisher.published(ReservationEndedIE.class)).containsExactly(new ReservationEndedIE(
                cancelled, CUSTOMER, PROVIDER, "CANCELLED", "CANCELLED_FOR_SERIOUS_FLAG", true, START, END, now));
        assertThat(world.publisher.published(ReservationSettledIE.class))
                .containsExactly(new ReservationSettledIE(cancelled, PROVIDER, Money.ZERO, "JOB", now));
        assertThat(world.publisher.published(ReservationRestoredIE.class))
                .containsExactly(new ReservationRestoredIE(restored, CUSTOMER, PROVIDER, now));
    }

    @Test
    void aFlaggedReservationNobodyDecidedIsCancelledAtTheBookedStart() {
        String id = world.reserved();
        new HandleBusinessSeriouslyFlaggedIE(world.standings, world.reservations, world.publisher, world.clock)
                .handle(new BusinessSeriouslyFlaggedIE(PROVIDER, NOW));
        SystemCancelUndecidedFlagged usecase =
                new SystemCancelUndecidedFlagged(world.reservations, world.publisher, world.clock);

        assertThat(usecase.cancelDue().ids()).isEmpty();
        world.clock.set(START);
        assertThat(usecase.cancelDue().ids()).containsExactly(id);

        assertThat(world.reservation(id).status()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(world.reservations.timesSaved(id)).isEqualTo(5);
        assertThat(world.publisher.published(ReservationSettledIE.class))
                .containsExactly(new ReservationSettledIE(id, PROVIDER, Money.ZERO, "JOB", START));
    }

    @Test
    void theCustomerChangesAReservationsAccessNotes() {
        String id = world.reserved();

        new CustomerChangeAccessNotes(world.reservations, world.publisher, world.clock)
                .change(new ChangeAccessNotesRequest(CUSTOMER, id, "Gate B, lift 2"));

        assertThat(world.reservation(id).state().accessNotes()).isEqualTo("Gate B, lift 2");
        assertThat(world.reservations.timesSaved(id)).isEqualTo(4);
    }

    @Test
    void theCustomerSeesTheirReservation() {
        String id = world.completionSubmitted();

        ViewCustomerReservationResponse view = new CustomerViewReservation(world.reservations)
                .view(new ViewCustomerReservationRequest(CUSTOMER, id));

        assertThat(view).isEqualTo(new ViewCustomerReservationResponse(id, "RESERVED", null, "Aisyah binti Rahman",
                "12 Jalan Teknokrat 5", "Unit 12-3, register at the guardhouse", Set.of(KITCHEN), START, END, 2, false,
                Money.ofRinggit(190), Money.ofSen(2850), Money.ofSen(21850), true, Set.of("helper-1", "helper-2"),
                START, END.plus(Duration.ofHours(24))));
        assertThatThrownBy(() -> new CustomerViewReservation(world.reservations)
                .view(new ViewCustomerReservationRequest("customer-2", id))).isInstanceOf(ReservationNotFoundException.class);
    }

    @Test
    void theProviderSeesARequestOnlyOncePaidAndTheAccessNotesOnlyFromAcceptance() {
        ProviderViewReservation usecase = new ProviderViewReservation(world.reservations);
        String unpaid = world.requested(START.plus(Duration.ofDays(2)));
        assertThatThrownBy(() -> usecase.view(new ViewProviderReservationRequest(PROVIDER, unpaid)))
                .isInstanceOf(ReservationNotFoundException.class);
        String paid = world.secured(START.plus(Duration.ofDays(1)));
        String accepted = world.reserved();
        ViewProviderReservationResponse request = usecase.view(new ViewProviderReservationRequest(PROVIDER, paid));
        assertThat(request.status()).isEqualTo("REQUESTED");
        assertThat(request.accessNotes()).isNull();
        assertThat(request.teamMemberIds()).isEmpty();
        assertThat(request.disputeWindowEndsAt()).isNull();
        assertThat(request.checkedInAt()).isNull();

        ViewProviderReservationResponse reserved = usecase.view(new ViewProviderReservationRequest(PROVIDER, accepted));
        assertThat(reserved.accessNotes()).isEqualTo("Unit 12-3, register at the guardhouse");
        assertThat(reserved.customerName()).isEqualTo("Aisyah binti Rahman");
        assertThat(reserved.total()).isEqualTo(Money.ofSen(21850));
        assertThatThrownBy(() -> usecase.view(new ViewProviderReservationRequest("provider-2", accepted)))
                .isInstanceOf(ReservationNotFoundException.class);
    }

    @Test
    void anEndedReservationShowsHowItEnded() {
        String id = world.reserved();
        new ProviderCancelReservation(world.reservations, world.publisher, world.clock)
                .cancel(new reservation.usecase.dto.CancelByProviderRequest(PROVIDER, id));

        assertThat(new CustomerViewReservation(world.reservations).view(new ViewCustomerReservationRequest(CUSTOMER, id))
                .ending()).isEqualTo("CANCELLED_BY_PROVIDER");
    }
}
