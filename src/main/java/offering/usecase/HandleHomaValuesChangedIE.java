package offering.usecase;

import ie.events.HomaValuesChangedIE;
import ie.events.OfferingAdjustedIE;
import java.time.Clock;
import java.time.Instant;
import offering.domain.HoursRange;
import offering.domain.Listing;
import offering.domain.Offering;
import offering.usecase.port.in.HomaValuesChangedIEHandler;
import offering.usecase.port.out.HomaHoursRepository;
import offering.usecase.port.out.OfferingIEPublisher;
import offering.usecase.port.out.OfferingRepository;

public final class HandleHomaValuesChangedIE implements HomaValuesChangedIEHandler {

    static final String ADJUSTMENT = "HOURS_NARROWED";

    private final HomaHoursRepository homaHours;
    private final OfferingRepository offerings;
    private final OfferingIEPublisher publisher;
    private final Clock clock;

    public HandleHomaValuesChangedIE(HomaHoursRepository homaHours, OfferingRepository offerings,
                                     OfferingIEPublisher publisher, Clock clock) {
        this.homaHours = homaHours;
        this.offerings = offerings;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Override
    public void handle(HomaValuesChangedIE event) {
        Instant now = clock.instant();
        HoursRange homaRange = new HoursRange(event.minimumHours(), event.maximumHours());
        homaHours.save(homaRange);
        for (Offering offering : offerings.all()) {
            Listing before = offering.listing();
            if (offering.fitHoursInto(homaRange)) {
                Offerings.saveAndAnnounce(offering, before, offerings, publisher, now);
                publisher.publish(new OfferingAdjustedIE(offering.providerId(), ADJUSTMENT, now));
            }
        }
    }
}
