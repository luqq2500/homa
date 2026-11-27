package com.homa.reservation.usecase.ie;

import com.homa.ie.events.PaymentHeldIE;
import com.homa.ie.port.ReservationIE;
import com.homa.reservation.domain.Checkout;
import com.homa.reservation.domain.CheckoutId;
import com.homa.reservation.domain.Reservation;
import com.homa.reservation.domain.ReservationId;
import com.homa.reservation.domain.exception.BookingRefusedException;
import com.homa.reservation.domain.replica.BookableProvider;
import com.homa.reservation.domain.replica.CustomerStanding;
import com.homa.reservation.domain.schedule.CustomerSchedule;
import com.homa.reservation.domain.schedule.ProviderSchedule;
import com.homa.reservation.usecase.BookableProviders;
import com.homa.reservation.usecase.ReservationEvents;
import com.homa.reservation.usecase.ScheduleRetry;
import com.homa.reservation.usecase.exception.CheckoutNotFoundException;
import com.homa.reservation.usecase.exception.CustomerNotFoundException;
import com.homa.reservation.usecase.port.in.PaymentHeldIEHandler;
import com.homa.reservation.usecase.port.out.CheckoutRepository;
import com.homa.reservation.usecase.port.out.CustomerScheduleRepository;
import com.homa.reservation.usecase.port.out.CustomerStandingRepository;
import com.homa.reservation.usecase.port.out.ProcessedMessageStore;
import com.homa.reservation.usecase.port.out.ProviderScheduleRepository;
import com.homa.reservation.usecase.port.out.ReservationIEPublisher;
import com.homa.reservation.usecase.port.out.ReservationRepository;
import java.util.List;
import java.util.stream.Stream;

/**
 * RES-7, RES-11, ACC-1: the checkout's payment is confirmed and held. If the time is still free and every booking
 * rule still passes, the reservation is Requested, both schedules are blocked in the same save, and Requested is
 * published; under Instant it is Reserved at once and Reserved is published too. Otherwise the checkout is
 * rejected, so Payment refunds it in full at once. When another payment's save got there first, the schedules are
 * reloaded and the retry finds the time taken.
 */
public final class HandlePaymentHeldIE implements PaymentHeldIEHandler {

    static final String HANDLER = "reservation.PaymentHeld";

    private final ProcessedMessageStore processed;
    private final CheckoutRepository checkouts;
    private final CustomerStandingRepository customers;
    private final BookableProviders providers;
    private final ProviderScheduleRepository providerSchedules;
    private final CustomerScheduleRepository customerSchedules;
    private final ReservationRepository reservations;
    private final ReservationIEPublisher publisher;
    private final ReservationEvents events;

    public HandlePaymentHeldIE(ProcessedMessageStore processed, CheckoutRepository checkouts,
            CustomerStandingRepository customers, BookableProviders providers,
            ProviderScheduleRepository providerSchedules, CustomerScheduleRepository customerSchedules,
            ReservationRepository reservations, ReservationIEPublisher publisher, ReservationEvents events) {
        this.processed = processed;
        this.checkouts = checkouts;
        this.customers = customers;
        this.providers = providers;
        this.providerSchedules = providerSchedules;
        this.customerSchedules = customerSchedules;
        this.reservations = reservations;
        this.publisher = publisher;
        this.events = events;
    }

    @Override
    public void handle(PaymentHeldIE event) {
        if (!processed.recordFirstHandling(event.eventId(), HANDLER)) {
            return;
        }
        Checkout checkout = checkouts.findById(new CheckoutId(event.checkoutId()))
                .orElseThrow(() -> new CheckoutNotFoundException(event.checkoutId()));
        ScheduleRetry.retried(() -> request(checkout, event)).forEach(publisher::publish);
    }

    /** One attempt: the events to publish once it is decided, Requested (and Reserved) or the rejection. */
    private List<ReservationIE> request(Checkout checkout, PaymentHeldIE event) {
        String customerId = checkout.booking().customerId();
        String providerId = checkout.booking().providerId();
        CustomerStanding customer = customers.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));
        BookableProvider provider = providers.find(providerId);
        ProviderSchedule providerSchedule = providerSchedules.findByProviderId(providerId)
                .orElseGet(() -> new ProviderSchedule(providerId));
        CustomerSchedule customerSchedule = customerSchedules.findByCustomerId(customerId)
                .orElseGet(() -> new CustomerSchedule(customerId));
        Reservation reservation;
        try {
            reservation = Reservation.request(ReservationId.generate(), checkout, provider, customer,
                    providerSchedule, customerSchedule, event.paidAt());
        } catch (BookingRefusedException refused) {
            return List.of(events.checkoutRejected(checkout, refused.rejection()));
        }
        reservations.saveWithSchedules(reservation, providerSchedule, customerSchedule);
        ReservationIE requested = events.requested(reservation);
        return Stream.concat(Stream.of(requested),
                reservation.reservedAt().map(reservedAt -> events.reserved(reservation)).stream()).toList();
    }
}
