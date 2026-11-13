package reservation.usecase;

import ie.events.EnquiryStartedIE;
import java.time.Clock;
import java.util.Optional;
import reservation.domain.Enquiry;
import reservation.usecase.dto.BookAgainRequest;
import reservation.usecase.dto.BookAgainResponse;
import reservation.usecase.exception.ReservationNotFoundException;
import reservation.usecase.port.in.CustomerAgainBooker;
import reservation.usecase.port.out.EnquiryRepository;
import reservation.usecase.port.out.ReservationIEPublisher;
import reservation.usecase.port.out.ReservationRepository;

public final class CustomerBookAgain implements CustomerAgainBooker {

    private final ReservationRepository reservations;
    private final EnquiryRepository enquiries;
    private final ReservationIEPublisher publisher;
    private final Clock clock;

    public CustomerBookAgain(ReservationRepository reservations, EnquiryRepository enquiries,
                             ReservationIEPublisher publisher, Clock clock) {
        this.reservations = reservations;
        this.enquiries = enquiries;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Override
    public BookAgainResponse bookAgain(BookAgainRequest request) {
        Enquiry again = fromPastReservation(request).or(() -> fromPastEnquiry(request))
                .orElseThrow(() -> new ReservationNotFoundException(request.pastId()));
        enquiries.save(again);
        publisher.publish(new EnquiryStartedIE(again.id(), again.customerId(), again.providerId(), clock.instant()));
        return new BookAgainResponse(again.id());
    }

    private Optional<Enquiry> fromPastReservation(BookAgainRequest request) {
        return reservations.find(request.pastId())
                .filter(past -> past.customerId().equals(request.customerId()))
                .map(past -> Enquiry.bookAgain(past.customerId(), past.providerId(), past.details(), request.start()));
    }

    private Optional<Enquiry> fromPastEnquiry(BookAgainRequest request) {
        return enquiries.find(request.pastId())
                .filter(past -> past.customerId().equals(request.customerId()))
                .map(past -> Enquiry.bookAgain(past.customerId(), past.providerId(), past.details(), request.start()));
    }
}
