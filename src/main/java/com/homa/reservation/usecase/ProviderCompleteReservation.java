package com.homa.reservation.usecase;

import com.homa.reservation.domain.CompletionPhoto;
import com.homa.reservation.domain.Reservation;
import com.homa.reservation.domain.schedule.CustomerSchedule;
import com.homa.reservation.domain.schedule.ProviderSchedule;
import com.homa.reservation.usecase.dto.CompleteReservationRequest;
import com.homa.reservation.usecase.dto.CompleteReservationResponse;
import com.homa.reservation.usecase.exception.CompletionPhotoNotFoundException;
import com.homa.reservation.usecase.port.in.ReservationCompleter;
import com.homa.reservation.usecase.port.out.CompletionPhotoStore;
import com.homa.reservation.usecase.port.out.CustomerScheduleRepository;
import com.homa.reservation.usecase.port.out.ProviderScheduleRepository;
import com.homa.reservation.usecase.port.out.ReservationIEPublisher;
import com.homa.reservation.usecase.port.out.ReservationRepository;
import com.homa.reservation.usecase.port.out.TimeSource;
import java.util.List;

/**
 * JOB-3: the provider marks the job Completed after its end, with at least one uploaded after-photo. Both parties'
 * time opens again (RES-9, RES-10), and the event carries the photos as evidence (JOB-6) and opens ratings (RTG-2).
 */
public final class ProviderCompleteReservation implements ReservationCompleter {

    private final ReservationRepository reservations;
    private final ProviderScheduleRepository providerSchedules;
    private final CustomerScheduleRepository customerSchedules;
    private final CompletionPhotoStore photos;
    private final ReservationIEPublisher publisher;
    private final ReservationEvents events;
    private final TimeSource time;

    public ProviderCompleteReservation(ReservationRepository reservations,
            ProviderScheduleRepository providerSchedules, CustomerScheduleRepository customerSchedules,
            CompletionPhotoStore photos, ReservationIEPublisher publisher, ReservationEvents events,
            TimeSource time) {
        this.reservations = reservations;
        this.providerSchedules = providerSchedules;
        this.customerSchedules = customerSchedules;
        this.photos = photos;
        this.publisher = publisher;
        this.events = events;
        this.time = time;
    }

    @Override
    public CompleteReservationResponse complete(CompleteReservationRequest request) {
        List<CompletionPhoto> afterPhotos = request.photoRefs().stream().map(this::uploaded).toList();
        Reservation completed = ScheduleRetry.retried(() -> markCompleted(request, afterPhotos));
        publisher.publish(events.completed(completed));
        return new CompleteReservationResponse(completed.id().value(), completed.status().name(),
                completed.completedAt().orElseThrow());
    }

    private Reservation markCompleted(CompleteReservationRequest request, List<CompletionPhoto> afterPhotos) {
        Reservation reservation = ReservationParties.requireProvider(
                ReservationParties.load(reservations, request.reservationId()), request.providerId());
        String providerId = reservation.booking().providerId();
        String customerId = reservation.booking().customerId();
        ProviderSchedule providerSchedule = providerSchedules.findByProviderId(providerId)
                .orElseGet(() -> new ProviderSchedule(providerId));
        CustomerSchedule customerSchedule = customerSchedules.findByCustomerId(customerId)
                .orElseGet(() -> new CustomerSchedule(customerId));
        reservation.markCompleted(afterPhotos, time.now());
        providerSchedule.release(reservation);
        customerSchedule.release(reservation);
        reservations.saveWithSchedules(reservation, providerSchedule, customerSchedule);
        return reservation;
    }

    private CompletionPhoto uploaded(String photoRef) {
        if (!photos.holds(photoRef)) {
            throw new CompletionPhotoNotFoundException(photoRef);
        }
        return new CompletionPhoto(photoRef);
    }
}
