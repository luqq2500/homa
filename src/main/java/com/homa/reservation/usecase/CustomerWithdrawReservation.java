package com.homa.reservation.usecase;

import com.homa.reservation.domain.Reservation;
import com.homa.reservation.domain.ReservationStatus;
import com.homa.reservation.domain.exception.ReservationStatusConflictException;
import com.homa.reservation.usecase.dto.WithdrawReservationRequest;
import com.homa.reservation.usecase.dto.WithdrawReservationResponse;
import com.homa.reservation.usecase.port.in.ReservationWithdrawer;
import com.homa.reservation.usecase.port.out.CustomerScheduleRepository;
import com.homa.reservation.usecase.port.out.ProviderScheduleRepository;
import com.homa.reservation.usecase.port.out.ReservationIEPublisher;
import com.homa.reservation.usecase.port.out.ReservationRepository;
import com.homa.reservation.usecase.port.out.TimeSource;

/**
 * REQ-1, REQ-2, REQ-5: the customer withdraws a Requested reservation for free (the one way to change a booking,
 * P2). Both schedules are released in the same save, so the time opens again, and Withdrawn is published for a full
 * refund. A withdrawal racing an acceptance resolves to whichever is saved first: when the acceptance won, whether
 * recorded earlier or saved first while this ran (the conflict reloads), nothing is withdrawn and the answer is
 * {@link WithdrawReservationResponse#ALREADY_ACCEPTED}, so the app offers cancellation instead.
 */
public final class CustomerWithdrawReservation implements ReservationWithdrawer {

    private final ReservationRepository reservations;
    private final CalendarRelease calendars;
    private final ReservationIEPublisher publisher;
    private final ReservationEvents events;
    private final TimeSource time;

    public CustomerWithdrawReservation(ReservationRepository reservations,
            ProviderScheduleRepository providerSchedules, CustomerScheduleRepository customerSchedules,
            ReservationIEPublisher publisher, ReservationEvents events, TimeSource time) {
        this.reservations = reservations;
        this.calendars = new CalendarRelease(reservations, providerSchedules, customerSchedules);
        this.publisher = publisher;
        this.events = events;
        this.time = time;
    }

    @Override
    public WithdrawReservationResponse withdraw(WithdrawReservationRequest request) {
        Reservation reservation = ScheduleRetry.retried(() -> withdrawnOrAccepted(request));
        if (reservation.status() == ReservationStatus.RESERVED) {
            return new WithdrawReservationResponse(reservation.id().value(),
                    WithdrawReservationResponse.ALREADY_ACCEPTED, reservation.status().name(), null);
        }
        publisher.publish(events.withdrawn(reservation));
        return new WithdrawReservationResponse(reservation.id().value(), WithdrawReservationResponse.WITHDRAWN,
                reservation.status().name(), reservation.withdrawnAt().orElseThrow());
    }

    /** The reservation withdrawn and saved, or as the provider's acceptance left it, Reserved. */
    private Reservation withdrawnOrAccepted(WithdrawReservationRequest request) {
        Reservation reservation = ReservationParties.requireCustomer(
                ReservationParties.load(reservations, request.reservationId()), request.customerId());
        try {
            reservation.withdraw(time.now());
        } catch (ReservationStatusConflictException refused) {
            if (reservation.status() == ReservationStatus.RESERVED) {
                return reservation;
            }
            throw refused;
        }
        calendars.saveReleased(reservation);
        return reservation;
    }
}
