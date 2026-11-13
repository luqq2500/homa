package reservation.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static reservation.support.ReservationFixtures.*;

import ie.events.EnquiryClosedIE;
import ie.events.EnquiryStartedIE;
import ie.events.PaymentFailedIE;
import ie.events.PaymentSecuredIE;
import ie.events.ProviderFinedIE;
import ie.events.ReservationAcceptedIE;
import ie.events.ReservationEndedIE;
import ie.events.ReservationRequestEditedIE;
import ie.events.ReservationRequestedIE;
import ie.events.ReservationSettledIE;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import kernel.Money;
import kernel.Percentage;
import org.junit.jupiter.api.Test;
import reservation.domain.AcceptancePolicy;
import reservation.domain.CancellationTier;
import reservation.domain.ReservationStatus;
import reservation.domain.exception.PaymentNotSecuredException;
import reservation.domain.exception.TimeNotFreeException;
import reservation.support.ReservationWorld;
import reservation.usecase.dto.AcceptRequestRequest;
import reservation.usecase.dto.AcceptRequestResponse;
import reservation.usecase.dto.BookAgainRequest;
import reservation.usecase.dto.CancelByCustomerRequest;
import reservation.usecase.dto.CancelByProviderRequest;
import reservation.usecase.dto.DeclineRequestRequest;
import reservation.usecase.dto.EditRequestRequest;
import reservation.usecase.dto.EditRequestResponse;
import reservation.usecase.dto.StartEnquiryRequest;
import reservation.usecase.dto.SubmitRequestRequest;
import reservation.usecase.dto.SubmitRequestResponse;
import reservation.usecase.exception.CustomerNotFoundException;
import reservation.usecase.exception.EnquiryNotFoundException;
import reservation.usecase.exception.HomaValuesNotKnownException;
import reservation.usecase.exception.ProviderNotFoundException;
import reservation.usecase.exception.ReservationNotFoundException;
import reservation.usecase.exception.UnknownChoiceException;

class RequestUsecasesTest {

    private final ReservationWorld world = new ReservationWorld();
    private final ProviderAcceptRequest accepter = new ProviderAcceptRequest(world.reservations, world.openTimes,
            world.standings, world.publisher, world.clock);
    private final HandlePaymentSecuredIE paymentSecured = new HandlePaymentSecuredIE(world.reservations,
            world.openTimes, world.standings, world.publisher, world.clock);

    private SubmitRequestRequest direct(String customerId, String providerId, String method) {
        return new SubmitRequestRequest(customerId, providerId, null, method, Set.of(KITCHEN), START, 3, 2);
    }

    @Test
    void aSubmittedRequestIsSavedPricedAndItsPaymentIsToBeSecured() {
        SubmitRequestResponse response = world.submitter().submit(direct(CUSTOMER, PROVIDER, "FPX"));

        assertThat(response.providerPrice()).isEqualTo(Money.ofRinggit(190));
        assertThat(response.serviceFee()).isEqualTo(Money.ofSen(2850));
        assertThat(response.total()).isEqualTo(Money.ofSen(21850));
        assertThat(response.supervisorIncluded()).isFalse();
        assertThat(response.expiresAt()).isEqualTo(NOW.plus(Duration.ofHours(24)));
        assertThat(world.reservation(response.reservationId()).status()).isEqualTo(ReservationStatus.REQUESTED);
        assertThat(world.publisher.published()).containsExactly(new ReservationRequestedIE(response.reservationId(),
                CUSTOMER, PROVIDER, null, "FPX", Money.ofRinggit(190), Money.ofSen(2850), Money.ofSen(21850),
                Percentage.of(15), START, END, NOW));
    }

    @Test
    void aRequestNeedsAKnownCustomerProviderPaymentMethodAndHomasValues() {
        assertThatThrownBy(() -> world.submitter().submit(direct("nobody", PROVIDER, "CARD")))
                .isInstanceOf(CustomerNotFoundException.class);
        assertThatThrownBy(() -> world.submitter().submit(direct(CUSTOMER, "nobody", "CARD")))
                .isInstanceOf(ProviderNotFoundException.class);
        assertThatThrownBy(() -> world.submitter().submit(direct(CUSTOMER, PROVIDER, "CASH")))
                .isInstanceOf(UnknownChoiceException.class);
        assertThatThrownBy(() -> world.submitter().submit(direct(CUSTOMER, PROVIDER, null)))
                .isInstanceOf(UnknownChoiceException.class);
        world.announcedValues = Optional.empty();
        assertThatThrownBy(() -> world.submitter().submit(direct(CUSTOMER, PROVIDER, "CARD")))
                .isInstanceOf(HomaValuesNotKnownException.class);
        assertThat(world.reservations.all()).isEmpty();
        assertThat(world.publisher.published()).isEmpty();
    }

    @Test
    void aRequestIsRefusedForATimeAnAcceptedReservationAlreadyTakes() {
        world.reserved();

        assertThatThrownBy(() -> world.requested(START.plus(Duration.ofHours(3)).plus(Duration.ofMinutes(15))))
                .isInstanceOf(TimeNotFreeException.class);
        assertThat(world.requested(START.plus(Duration.ofHours(3)).plus(Duration.ofMinutes(30)))).isNotBlank();
    }

    @Test
    void anEnquiryOpensAChatAndBecomesTheReservationWhenTheCustomerReserves() {
        String enquiryId = new CustomerStartEnquiry(world.enquiries, world.providerTerms, world.publisher, world.clock)
                .start(new StartEnquiryRequest(CUSTOMER, PROVIDER, Set.of(KITCHEN), START, 3, 2)).enquiryId();
        assertThat(world.publisher.published()).containsExactly(new EnquiryStartedIE(enquiryId, CUSTOMER, PROVIDER, NOW));

        String reservationId = world.submitter().submit(new SubmitRequestRequest(CUSTOMER, PROVIDER, enquiryId,
                "CARD", Set.of(KITCHEN, LAUNDRY), START, 2, 1)).reservationId();

        assertThat(world.enquiries.find(enquiryId).orElseThrow().reservationId()).isEqualTo(reservationId);
        assertThat(world.enquiries.timesSaved(enquiryId)).isEqualTo(2);
        assertThat(world.publisher.published(ReservationRequestedIE.class)).singleElement()
                .satisfies(requested -> assertThat(requested.enquiryId()).isEqualTo(enquiryId));
    }

    @Test
    void anEnquiryIsOnlyStartedWithAKnownProviderAndReservedFromByItsOwnCustomer() {
        CustomerStartEnquiry starter =
                new CustomerStartEnquiry(world.enquiries, world.providerTerms, world.publisher, world.clock);
        assertThatThrownBy(() -> starter.start(new StartEnquiryRequest(CUSTOMER, "nobody", Set.of(KITCHEN), START, 3, 2)))
                .isInstanceOf(ProviderNotFoundException.class);
        String someoneElses = starter.start(new StartEnquiryRequest("customer-2", PROVIDER, Set.of(KITCHEN), START, 3, 2))
                .enquiryId();

        assertThatThrownBy(() -> world.submitter().submit(new SubmitRequestRequest(CUSTOMER, PROVIDER, someoneElses,
                "CARD", Set.of(KITCHEN), START, 3, 2))).isInstanceOf(EnquiryNotFoundException.class);
        assertThatThrownBy(() -> world.submitter().submit(new SubmitRequestRequest(CUSTOMER, PROVIDER, "none",
                "CARD", Set.of(KITCHEN), START, 3, 2))).isInstanceOf(EnquiryNotFoundException.class);
        assertThat(world.reservations.all()).isEmpty();
    }

    @Test
    void bookAgainStartsANewEnquiryFromAPastReservationOrEnquiry() {
        String pastReservation = world.requested();
        CustomerBookAgain usecase =
                new CustomerBookAgain(world.reservations, world.enquiries, world.publisher, world.clock);
        Instant nextWeek = START.plus(Duration.ofDays(7));

        String fromReservation = usecase.bookAgain(new BookAgainRequest(CUSTOMER, pastReservation, nextWeek)).enquiryId();
        String fromEnquiry = usecase.bookAgain(new BookAgainRequest(CUSTOMER, fromReservation, nextWeek.plusSeconds(900)))
                .enquiryId();

        assertThat(world.enquiries.find(fromReservation).orElseThrow().details().start()).isEqualTo(nextWeek);
        assertThat(world.enquiries.find(fromReservation).orElseThrow().details().jobIds()).containsExactly(KITCHEN);
        assertThat(world.enquiries.find(fromEnquiry).orElseThrow().providerId()).isEqualTo(PROVIDER);
        assertThat(world.publisher.published(EnquiryStartedIE.class)).hasSize(2).last()
                .isEqualTo(new EnquiryStartedIE(fromEnquiry, CUSTOMER, PROVIDER, NOW));
        assertThatThrownBy(() -> usecase.bookAgain(new BookAgainRequest("customer-2", pastReservation, nextWeek)))
                .isInstanceOf(ReservationNotFoundException.class);
        assertThatThrownBy(() -> usecase.bookAgain(new BookAgainRequest("customer-2", fromReservation, nextWeek)))
                .isInstanceOf(ReservationNotFoundException.class);
    }

    @Test
    void enquiriesThatWereNeverRequestedCloseWhenTheirDatePasses() {
        CustomerStartEnquiry starter =
                new CustomerStartEnquiry(world.enquiries, world.providerTerms, world.publisher, world.clock);
        String passed = starter.start(new StartEnquiryRequest(CUSTOMER, PROVIDER, Set.of(KITCHEN), START, 3, 2)).enquiryId();
        String later = starter.start(new StartEnquiryRequest(CUSTOMER, PROVIDER, Set.of(KITCHEN), START.plusSeconds(900), 3, 2))
                .enquiryId();
        world.clock.set(START);

        assertThat(new SystemCloseEnquiries(world.enquiries, world.publisher, world.clock).closePassed().ids())
                .containsExactly(passed);

        assertThat(world.enquiries.timesSaved(passed)).isEqualTo(2);
        assertThat(world.enquiries.timesSaved(later)).isEqualTo(1);
        assertThat(world.publisher.published(EnquiryClosedIE.class)).containsExactly(new EnquiryClosedIE(passed, START));
    }

    @Test
    void anEditIsSavedAndAnnouncedWithItsNewPrice() {
        String id = world.secured(START);
        CustomerEditRequest usecase = new CustomerEditRequest(world.reservations, world.openTimes, world.standings,
                world.publisher, world.clock);

        EditRequestResponse response = usecase.edit(new EditRequestRequest(CUSTOMER, id, Set.of(KITCHEN), START, 3, 3));

        assertThat(response).isEqualTo(new EditRequestResponse(id, Money.ofSen(32200), false));
        assertThat(world.reservations.timesSaved(id)).isEqualTo(3);
        assertThat(world.publisher.published(ReservationRequestEditedIE.class)).containsExactly(
                new ReservationRequestEditedIE(id, CUSTOMER, PROVIDER, Money.ofRinggit(280), Money.ofRinggit(42),
                        Money.ofSen(32200), START, END, NOW));
        assertThatThrownBy(() -> usecase.edit(new EditRequestRequest("customer-2", id, Set.of(KITCHEN), START, 3, 3)))
                .isInstanceOf(ReservationNotFoundException.class);
    }

    @Test
    void aRequestsOwnTimeDoesNotBlockItsEdit() {
        String id = world.reserved();
        String other = world.secured(START.plus(Duration.ofHours(4)));

        new CustomerEditRequest(world.reservations, world.openTimes, world.standings, world.publisher, world.clock)
                .edit(new EditRequestRequest(CUSTOMER, other, Set.of(KITCHEN), START.plus(Duration.ofHours(5)), 3, 2));

        assertThat(world.reservation(other).details().start()).isEqualTo(START.plus(Duration.ofHours(5)));
        assertThat(world.reservation(id).status()).isEqualTo(ReservationStatus.RESERVED);
    }

    @Test
    void securedPaymentLetsTheProviderAcceptARequestAndAcceptWithoutItIsRefused() {
        String id = world.requested();

        assertThatThrownBy(() -> accepter.accept(new AcceptRequestRequest(PROVIDER, id)))
                .isInstanceOf(PaymentNotSecuredException.class);
        paymentSecured.handle(new PaymentSecuredIE(id, NOW));
        paymentSecured.handle(new PaymentSecuredIE("unknown", NOW));

        assertThat(world.reservation(id).isPaymentSecured()).isTrue();
        assertThat(world.reservation(id).status()).isEqualTo(ReservationStatus.REQUESTED);
        assertThat(world.reservations.timesSaved(id)).isEqualTo(2);
        assertThat(world.publisher.published(ReservationAcceptedIE.class)).isEmpty();
    }

    @Test
    void anAcceptedRequestIsAnnouncedAndHoldsTheProvidersTime() {
        String id = world.secured(START);

        AcceptRequestResponse response = accepter.accept(new AcceptRequestRequest(PROVIDER, id));

        assertThat(response.reservationId()).isEqualTo(id);
        assertThat(response.declinedForClash()).isEmpty();
        assertThat(world.reservation(id).status()).isEqualTo(ReservationStatus.RESERVED);
        assertThat(world.reservations.timesSaved(id)).isEqualTo(3);
        assertThat(world.publisher.published(ReservationAcceptedIE.class))
                .containsExactly(new ReservationAcceptedIE(id, CUSTOMER, PROVIDER, START, END, false, NOW));
        assertThatThrownBy(() -> accepter.accept(new AcceptRequestRequest("provider-2", id)))
                .isInstanceOf(ReservationNotFoundException.class);
    }

    @Test
    void acceptingDeclinesTheOtherOpenRequestsThatNowClashAndCostsThoseCustomersNothing() {
        String accepted = world.secured(START);
        String clashing = world.secured(START.plus(Duration.ofHours(1)));
        String insideBuffer = world.requested(END.plus(Duration.ofMinutes(15)));
        String clear = world.secured(END.plus(Duration.ofMinutes(30)));

        AcceptRequestResponse response = accepter.accept(new AcceptRequestRequest(PROVIDER, accepted));

        assertThat(response.declinedForClash()).containsExactlyInAnyOrder(clashing, insideBuffer);
        assertThat(world.reservation(clashing).status()).isEqualTo(ReservationStatus.DECLINED);
        assertThat(world.reservation(insideBuffer).status()).isEqualTo(ReservationStatus.DECLINED);
        assertThat(world.reservation(clear).status()).isEqualTo(ReservationStatus.REQUESTED);
        assertThat(world.reservations.timesSaved(clashing)).isEqualTo(3);
        assertThat(world.publisher.published(ReservationEndedIE.class)).extracting(ReservationEndedIE::ending)
                .containsExactly("DECLINED_FOR_CLASH", "DECLINED_FOR_CLASH");
        assertThat(world.publisher.published(ReservationSettledIE.class)).hasSize(2)
                .allSatisfy(settled -> assertThat(settled.providerReceives()).isEqualTo(Money.ZERO));
    }

    @Test
    void anInstantBookRequestIsAcceptedTheMomentItsPaymentIsSecured() {
        world.offering(terms(CancellationTier.STANDARD, AcceptancePolicy.INSTANT_BOOK));
        String id = world.requested();
        String clashing = world.requested(START.plus(Duration.ofHours(1)));

        paymentSecured.handle(new PaymentSecuredIE(id, NOW));

        assertThat(world.reservation(id).status()).isEqualTo(ReservationStatus.RESERVED);
        assertThat(world.reservations.timesSaved(id)).isEqualTo(2);
        assertThat(world.publisher.published(ReservationAcceptedIE.class))
                .containsExactly(new ReservationAcceptedIE(id, CUSTOMER, PROVIDER, START, END, true, NOW));
        assertThat(world.reservation(clashing).status()).isEqualTo(ReservationStatus.DECLINED);
    }

    @Test
    void anInstantBookRequestWhoseTimeIsNoLongerFreeEndsAndCostsNothing() {
        world.offering(terms(CancellationTier.STANDARD, AcceptancePolicy.INSTANT_BOOK));
        String first = world.requested();
        String second = world.requested();
        paymentSecured.handle(new PaymentSecuredIE(first, NOW));

        paymentSecured.handle(new PaymentSecuredIE(second, NOW));

        assertThat(world.reservation(second).status()).isEqualTo(ReservationStatus.DECLINED);
        assertThat(world.publisher.published(ReservationEndedIE.class)).last()
                .isEqualTo(new ReservationEndedIE(second, CUSTOMER, PROVIDER, "DECLINED", "DECLINED_FOR_CLASH", false,
                        START, END, NOW));
        assertThat(world.publisher.published(ReservationAcceptedIE.class)).hasSize(1);
    }

    @Test
    void aFailedPaymentEndsTheRequestAndMovesNoMoney() {
        String id = world.requested();
        HandlePaymentFailedIE handler = new HandlePaymentFailedIE(world.reservations, world.publisher, world.clock);

        handler.handle(new PaymentFailedIE(id, NOW));
        handler.handle(new PaymentFailedIE(id, NOW));
        handler.handle(new PaymentFailedIE("unknown", NOW));

        assertThat(world.reservation(id).status()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(world.reservations.timesSaved(id)).isEqualTo(2);
        assertThat(world.publisher.published(ReservationEndedIE.class)).containsExactly(new ReservationEndedIE(id,
                CUSTOMER, PROVIDER, "CANCELLED", "PAYMENT_FAILED", false, START, END, NOW));
        assertThat(world.publisher.published(ReservationSettledIE.class)).isEmpty();
    }

    @Test
    void aDeclinedRequestIsAnnouncedAndRefundedInFull() {
        String id = world.secured(START);

        new ProviderDeclineRequest(world.reservations, world.publisher, world.clock)
                .decline(new DeclineRequestRequest(PROVIDER, id));

        assertThat(world.reservation(id).status()).isEqualTo(ReservationStatus.DECLINED);
        assertThat(world.reservations.timesSaved(id)).isEqualTo(3);
        assertThat(world.publisher.published(ReservationEndedIE.class)).containsExactly(new ReservationEndedIE(id,
                CUSTOMER, PROVIDER, "DECLINED", "DECLINED_BY_PROVIDER", false, START, END, NOW));
        assertThat(world.publisher.published(ReservationSettledIE.class))
                .containsExactly(new ReservationSettledIE(id, PROVIDER, Money.ZERO, "JOB", NOW));
    }

    @Test
    void unansweredRequestsExpire() {
        String id = world.secured(START);
        String later = world.secured(START.plus(Duration.ofHours(4)));
        SystemExpireRequests usecase = new SystemExpireRequests(world.reservations, world.publisher, world.clock);

        assertThat(usecase.expireDue().ids()).isEmpty();
        world.clock.set(NOW.plus(Duration.ofHours(24)));

        assertThat(usecase.expireDue().ids()).containsExactlyInAnyOrder(id, later);
        assertThat(world.reservation(id).status()).isEqualTo(ReservationStatus.EXPIRED);
        assertThat(world.reservations.timesSaved(id)).isEqualTo(3);
        assertThat(world.publisher.published(ReservationEndedIE.class)).extracting(ReservationEndedIE::ending)
                .containsExactly("EXPIRED", "EXPIRED");
    }

    @Test
    void aCustomerCancellationInsideTheFeeWindowSplitsTheMoney() {
        String id = world.reserved();
        world.clock.set(START.minus(Duration.ofHours(2)));
        CustomerCancelReservation usecase =
                new CustomerCancelReservation(world.reservations, world.publisher, world.clock);

        assertThat(usecase.cancel(new CancelByCustomerRequest(CUSTOMER, id)).providerReceives())
                .isEqualTo(Money.ofRinggit(18));

        assertThat(world.reservations.timesSaved(id)).isEqualTo(4);
        assertThat(world.publisher.published(ReservationEndedIE.class)).containsExactly(new ReservationEndedIE(id,
                CUSTOMER, PROVIDER, "CANCELLED", "CANCELLED_BY_CUSTOMER", true, START, END, world.clock.instant()));
        assertThat(world.publisher.published(ReservationSettledIE.class)).containsExactly(
                new ReservationSettledIE(id, PROVIDER, Money.ofRinggit(18), "CANCELLATION_FEE", world.clock.instant()));
        assertThat(world.publisher.published(ProviderFinedIE.class)).isEmpty();
    }

    @Test
    void aLateProviderCancellationRefundsTheCustomerAndFinesTheProvider() {
        String id = world.reserved();
        world.clock.set(START.minus(Duration.ofHours(2)));

        Money fine = new ProviderCancelReservation(world.reservations, world.publisher, world.clock)
                .cancel(new CancelByProviderRequest(PROVIDER, id)).fine();

        assertThat(fine).isEqualTo(Money.ofRinggit(36));
        assertThat(world.reservations.timesSaved(id)).isEqualTo(4);
        assertThat(world.publisher.published(ReservationSettledIE.class)).containsExactly(
                new ReservationSettledIE(id, PROVIDER, Money.ZERO, "JOB", world.clock.instant()));
        assertThat(world.publisher.published(ProviderFinedIE.class)).containsExactly(
                new ProviderFinedIE(id, PROVIDER, Money.ofRinggit(36), "LATE_CANCELLATION", world.clock.instant()));
    }

    @Test
    void anEarlyProviderCancellationCarriesNoFine() {
        String id = world.reserved();

        Money fine = new ProviderCancelReservation(world.reservations, world.publisher, world.clock)
                .cancel(new CancelByProviderRequest(PROVIDER, id)).fine();

        assertThat(fine).isEqualTo(Money.ZERO);
        assertThat(world.publisher.published(ProviderFinedIE.class)).isEmpty();
    }
}
