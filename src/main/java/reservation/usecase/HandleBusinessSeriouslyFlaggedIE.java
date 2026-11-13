package reservation.usecase;

import ie.events.BusinessSeriouslyFlaggedIE;
import ie.events.ReservationDisputedForFlagIE;
import java.time.Clock;
import java.time.Instant;
import reservation.domain.ProviderStanding;
import reservation.domain.Reservation;
import reservation.usecase.port.in.BusinessSeriouslyFlaggedIEHandler;
import reservation.usecase.port.out.ProviderStandingRepository;
import reservation.usecase.port.out.ReservationIEPublisher;
import reservation.usecase.port.out.ReservationRepository;

public final class HandleBusinessSeriouslyFlaggedIE implements BusinessSeriouslyFlaggedIEHandler {

    private final ProviderStandingRepository standings;
    private final ReservationRepository reservations;
    private final ReservationIEPublisher publisher;
    private final Clock clock;

    public HandleBusinessSeriouslyFlaggedIE(ProviderStandingRepository standings, ReservationRepository reservations, ReservationIEPublisher publisher, Clock clock) {
        this.standings = standings;
        this.reservations = reservations;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Override
    public void handle(BusinessSeriouslyFlaggedIE event) {
        ProviderStanding standing = standings.find(event.businessId())
                .orElseGet(() -> ProviderStanding.unverified(event.businessId()));
        standings.save(new ProviderStanding(standing.providerId(), standing.verifiedUntil(), true));
        Instant now = clock.instant();
        for (Reservation reservation : reservations.ofProvider(event.businessId())) {
            if (reservation.disputeForSeriousFlag()) {
                reservations.save(reservation);
                publisher.publish(new ReservationDisputedForFlagIE(reservation.id(), reservation.customerId(),
                        reservation.providerId(), reservation.details().start(), now));
            }
        }
    }
}
