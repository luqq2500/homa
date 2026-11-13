package reservation.usecase;

import ie.events.ReservationRequestedIE;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import reservation.domain.BookingDetails;
import reservation.domain.Customer;
import reservation.domain.Enquiry;
import reservation.domain.HomaValues;
import reservation.domain.PaymentMethod;
import reservation.domain.PriceBreakdown;
import reservation.domain.ProviderTerms;
import reservation.domain.Reservation;
import reservation.usecase.dto.SubmitRequestRequest;
import reservation.usecase.dto.SubmitRequestResponse;
import reservation.usecase.exception.CustomerNotFoundException;
import reservation.usecase.exception.EnquiryNotFoundException;
import reservation.usecase.exception.HomaValuesNotKnownException;
import reservation.usecase.exception.ProviderNotFoundException;
import reservation.usecase.exception.UnknownChoiceException;
import reservation.usecase.port.in.CustomerRequestSubmitter;
import reservation.usecase.port.out.CustomerRepository;
import reservation.usecase.port.out.EnquiryRepository;
import reservation.usecase.port.out.HomaValuesRepository;
import reservation.usecase.port.out.ProviderOpenTimeRepository;
import reservation.usecase.port.out.ProviderStandingRepository;
import reservation.usecase.port.out.ProviderTermsRepository;
import reservation.usecase.port.out.ReservationIEPublisher;
import reservation.usecase.port.out.ReservationRepository;

public final class CustomerSubmitRequest implements CustomerRequestSubmitter {

    private final ReservationRepository reservations;
    private final EnquiryRepository enquiries;
    private final CustomerRepository customers;
    private final ProviderTermsRepository providerTerms;
    private final HomaValuesRepository homaValues;
    private final ProviderScheduleReader schedules;
    private final ReservationIEPublisher publisher;
    private final Clock clock;

    public CustomerSubmitRequest(ReservationRepository reservations, EnquiryRepository enquiries,
                                 CustomerRepository customers, ProviderTermsRepository providerTerms,
                                 ProviderStandingRepository standings, ProviderOpenTimeRepository openTimes,
                                 HomaValuesRepository homaValues, ReservationIEPublisher publisher, Clock clock) {
        this.reservations = reservations;
        this.enquiries = enquiries;
        this.customers = customers;
        this.providerTerms = providerTerms;
        this.homaValues = homaValues;
        this.schedules = new ProviderScheduleReader(openTimes, standings, reservations);
        this.publisher = publisher;
        this.clock = clock;
    }

    @Override
    public SubmitRequestResponse submit(SubmitRequestRequest request) {
        Instant now = clock.instant();
        PaymentMethod method = paymentMethod(request.paymentMethod());
        Customer customer = customers.find(request.customerId())
                .orElseThrow(() -> new CustomerNotFoundException(request.customerId()));
        ProviderTerms terms = providerTerms.find(request.providerId())
                .orElseThrow(() -> new ProviderNotFoundException(request.providerId()));
        HomaValues values = homaValues.current().orElseThrow(HomaValuesNotKnownException::new);
        Optional<Enquiry> enquiry = enquiryOf(request);
        BookingDetails details =
                new BookingDetails(request.jobIds(), request.start(), request.hours(), request.helpers());

        Reservation reservation = Reservation.request(customer, terms, schedules.standingOf(terms.providerId()),
                schedules.scheduleOf(terms.providerId(), null), details, values, now);
        enquiry.ifPresent(reservedFrom -> {
            reservedFrom.becomeReservation(reservation.id());
            enquiries.save(reservedFrom);
        });
        reservations.save(reservation);
        PriceBreakdown price = reservation.price();
        publisher.publish(new ReservationRequestedIE(reservation.id(), customer.customerId(), terms.providerId(),
                request.enquiryId(), method.name(), price.providerPrice(), price.serviceFee(), price.total(),
                values.serviceFee(), details.start(), details.end(), now));
        return new SubmitRequestResponse(reservation.id(), price.providerPrice(), price.serviceFee(), price.total(),
                reservation.supervisorIncluded(), reservation.expiresAt());
    }

    /** The enquiry the customer reserves from, if any: their own, with this provider. */
    private Optional<Enquiry> enquiryOf(SubmitRequestRequest request) {
        if (request.enquiryId() == null) {
            return Optional.empty();
        }
        Enquiry enquiry = enquiries.find(request.enquiryId())
                .filter(own -> own.customerId().equals(request.customerId()))
                .filter(own -> own.providerId().equals(request.providerId()))
                .orElseThrow(() -> new EnquiryNotFoundException(request.enquiryId()));
        return Optional.of(enquiry);
    }

    private static PaymentMethod paymentMethod(String name) {
        try {
            return PaymentMethod.valueOf(name);
        } catch (IllegalArgumentException | NullPointerException unknown) {
            throw new UnknownChoiceException(name);
        }
    }
}
