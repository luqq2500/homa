package reservation.usecase;

import reservation.domain.Reservation;
import reservation.domain.ReservationState;
import reservation.usecase.dto.ViewProviderReservationRequest;
import reservation.usecase.dto.ViewProviderReservationResponse;
import reservation.usecase.exception.ReservationNotFoundException;
import reservation.usecase.port.in.ProviderReservationViewer;
import reservation.usecase.port.out.ReservationRepository;

public final class ProviderViewReservation implements ProviderReservationViewer {

    private final ReservationRepository reservations;

    public ProviderViewReservation(ReservationRepository reservations) {
        this.reservations = reservations;
    }

    @Override
    public ViewProviderReservationResponse view(ViewProviderReservationRequest request) {
        Reservation reservation =
                OwnedReservations.ofProvider(reservations, request.providerId(), request.reservationId());
        if (reservation.isRequested() && !reservation.isPaymentSecured()) {
            throw new ReservationNotFoundException(request.reservationId());
        }
        ReservationViews view = ReservationViews.of(reservation);
        ReservationState booked = view.booked();
        return new ViewProviderReservationResponse(booked.id(), booked.status().name(), view.ending(), booked.customerName(),
                booked.address().line(), view.accessNotesForProvider(), booked.details().jobIds(), booked.details().start(),
                booked.details().end(), booked.details().helpers(), reservation.supervisorIncluded(),
                booked.price().providerPrice(), booked.price().serviceFee(), booked.price().total(),
                booked.paymentSecured(), view.teamMemberIds(), booked.checkedInAt(), view.disputeWindowEndsAt());
    }
}
