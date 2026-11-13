package reservation.usecase;

import reservation.domain.Reservation;
import reservation.domain.ReservationState;
import reservation.usecase.dto.ViewCustomerReservationRequest;
import reservation.usecase.dto.ViewCustomerReservationResponse;
import reservation.usecase.port.in.CustomerReservationViewer;
import reservation.usecase.port.out.ReservationRepository;

public final class CustomerViewReservation implements CustomerReservationViewer {

    private final ReservationRepository reservations;

    public CustomerViewReservation(ReservationRepository reservations) {
        this.reservations = reservations;
    }

    @Override
    public ViewCustomerReservationResponse view(ViewCustomerReservationRequest request) {
        Reservation reservation =
                OwnedReservations.ofCustomer(reservations, request.customerId(), request.reservationId());
        ReservationViews view = ReservationViews.of(reservation);
        ReservationState booked = view.booked();
        return new ViewCustomerReservationResponse(booked.id(), booked.status().name(), view.ending(), booked.customerName(),
                booked.address().line(), booked.accessNotes(), booked.details().jobIds(), booked.details().start(),
                booked.details().end(), booked.details().helpers(), reservation.supervisorIncluded(),
                booked.price().providerPrice(), booked.price().serviceFee(), booked.price().total(),
                booked.paymentSecured(), view.teamMemberIds(), booked.checkedInAt(), view.disputeWindowEndsAt());
    }
}
