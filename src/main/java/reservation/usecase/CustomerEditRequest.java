package reservation.usecase;

import ie.events.ReservationRequestEditedIE;
import java.time.Clock;
import java.time.Instant;
import reservation.domain.BookingDetails;
import reservation.domain.Reservation;
import reservation.usecase.dto.EditRequestRequest;
import reservation.usecase.dto.EditRequestResponse;
import reservation.usecase.port.in.CustomerRequestEditor;
import reservation.usecase.port.out.ProviderOpenTimeRepository;
import reservation.usecase.port.out.ProviderStandingRepository;
import reservation.usecase.port.out.ReservationIEPublisher;
import reservation.usecase.port.out.ReservationRepository;

public final class CustomerEditRequest implements CustomerRequestEditor {

    private final ReservationRepository reservations;
    private final ProviderOpenTimeRepository openTimes;
    private final ProviderStandingRepository standings;
    private final ReservationIEPublisher publisher;
    private final Clock clock;
    private final ProviderScheduleReader schedules;

    public CustomerEditRequest(ReservationRepository reservations, ProviderOpenTimeRepository openTimes, ProviderStandingRepository standings, ReservationIEPublisher publisher, Clock clock) {
        this.reservations = reservations;
        this.openTimes = openTimes;
        this.standings = standings;
        this.publisher = publisher;
        this.clock = clock;
        this.schedules = new ProviderScheduleReader(openTimes, standings, reservations);
    }

    @Override
    public EditRequestResponse edit(EditRequestRequest request) {
        Instant now = clock.instant();
        Reservation reservation =
                OwnedReservations.ofCustomer(reservations, request.customerId(), request.reservationId());
        BookingDetails details =
                new BookingDetails(request.jobIds(), request.start(), request.hours(), request.helpers());
        reservation.edit(details, schedules.standingOf(reservation.providerId()),
                schedules.scheduleOf(reservation.providerId(), reservation.id()), now);
        reservations.save(reservation);
        publisher.publish(new ReservationRequestEditedIE(reservation.id(), reservation.customerId(),
                reservation.providerId(), reservation.price().providerPrice(), reservation.price().serviceFee(),
                reservation.price().total(), details.start(), details.end(), now));
        return new EditRequestResponse(reservation.id(), reservation.price().total(), reservation.isPaymentSecured());
    }
}
