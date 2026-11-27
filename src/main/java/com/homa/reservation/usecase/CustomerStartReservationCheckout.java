package com.homa.reservation.usecase;

import com.homa.reservation.domain.Booking;
import com.homa.reservation.domain.Checkout;
import com.homa.reservation.domain.CheckoutId;
import com.homa.reservation.domain.JobSelection;
import com.homa.reservation.domain.JobTime;
import com.homa.reservation.domain.exception.BookingRefusedException;
import com.homa.reservation.domain.replica.BookableProvider;
import com.homa.reservation.domain.replica.CustomerStanding;
import com.homa.reservation.domain.schedule.CustomerSchedule;
import com.homa.reservation.domain.schedule.ProviderSchedule;
import com.homa.reservation.usecase.dto.StartReservationCheckoutRequest;
import com.homa.reservation.usecase.dto.StartReservationCheckoutResponse;
import com.homa.reservation.usecase.exception.CustomerNotFoundException;
import com.homa.reservation.usecase.port.in.ReservationCheckoutStarter;
import com.homa.reservation.usecase.port.out.CheckoutRepository;
import com.homa.reservation.usecase.port.out.CustomerScheduleRepository;
import com.homa.reservation.usecase.port.out.CustomerStandingRepository;
import com.homa.reservation.usecase.port.out.ProviderScheduleRepository;
import com.homa.reservation.usecase.port.out.ReservationIEPublisher;
import com.homa.reservation.usecase.port.out.TimeSource;
import java.time.Duration;
import java.util.Set;

/**
 * RES-1 to RES-5, RES-9, RES-10, ACC-1, ACC-3: the customer starts paying for a booking at their home address. The
 * booking rules pass and both parties' time is free now, but nothing is held: the first confirmed payment wins
 * (RES-11). A customer who must verify their identity first is asked to, and no payment opens (CUS-3); any other
 * refusal is the domain's {@link BookingRefusedException}.
 */
public final class CustomerStartReservationCheckout implements ReservationCheckoutStarter {

    static final String STARTED = "STARTED";
    static final String VERIFICATION_REQUIRED = "VERIFICATION_REQUIRED";

    private final CustomerStandingRepository customers;
    private final BookableProviders providers;
    private final ProviderScheduleRepository providerSchedules;
    private final CustomerScheduleRepository customerSchedules;
    private final CheckoutRepository checkouts;
    private final ReservationIEPublisher publisher;
    private final ReservationEvents events;
    private final TimeSource time;

    public CustomerStartReservationCheckout(CustomerStandingRepository customers, BookableProviders providers,
            ProviderScheduleRepository providerSchedules, CustomerScheduleRepository customerSchedules,
            CheckoutRepository checkouts, ReservationIEPublisher publisher, ReservationEvents events,
            TimeSource time) {
        this.customers = customers;
        this.providers = providers;
        this.providerSchedules = providerSchedules;
        this.customerSchedules = customerSchedules;
        this.checkouts = checkouts;
        this.publisher = publisher;
        this.events = events;
        this.time = time;
    }

    @Override
    public StartReservationCheckoutResponse start(StartReservationCheckoutRequest request) {
        CustomerStanding customer = customers.findById(request.customerId())
                .orElseThrow(() -> new CustomerNotFoundException(request.customerId()));
        BookableProvider provider = providers.find(request.providerId());
        ProviderSchedule providerSchedule = providerSchedules.findByProviderId(provider.providerId())
                .orElseGet(() -> new ProviderSchedule(provider.providerId()));
        CustomerSchedule customerSchedule = customerSchedules.findByCustomerId(customer.customerId())
                .orElseGet(() -> new CustomerSchedule(customer.customerId()));
        Booking booking = new Booking(customer.customerId(), provider.providerId(),
                new JobSelection(Set.copyOf(request.jobCodes())),
                JobTime.of(request.jobStart(), Duration.ofMinutes(request.minutes())), request.helpers(),
                customer.homeAddress());
        Checkout checkout;
        try {
            checkout = Checkout.start(CheckoutId.generate(), booking, provider, customer, providerSchedule,
                    customerSchedule, time.now());
        } catch (BookingRefusedException refused) {
            if (refused.refusal().asksForVerification()) {
                return new StartReservationCheckoutResponse(VERIFICATION_REQUIRED, null, null);
            }
            throw refused;
        }
        checkouts.save(checkout);
        publisher.publish(events.checkoutStarted(checkout));
        return new StartReservationCheckoutResponse(STARTED, checkout.id().value(), checkout.quote().total());
    }
}
