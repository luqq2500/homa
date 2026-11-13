package offering.support;

import static offering.support.OfferingFixtures.HOMA_HOURS;
import static offering.support.OfferingFixtures.NOW;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Optional;
import offering.domain.HoursRange;
import offering.domain.OfferableCatalogue;
import offering.domain.Offering;
import offering.usecase.port.out.HomaHoursRepository;
import offering.usecase.port.out.OfferableCatalogueRepository;

/** The offering context's ports, faked in memory, with shortcuts to reach the state a test needs. */
public final class OfferingWorld {

    public final InMemoryOfferingRepository offerings = new InMemoryOfferingRepository();
    public final RecordingOfferingIEPublisher publisher = new RecordingOfferingIEPublisher();
    public final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    public OfferableCatalogue catalogue = OfferingFixtures.catalogue();
    public int catalogueSaves;
    public final OfferableCatalogueRepository catalogues = new OfferableCatalogueRepository() {
        @Override
        public OfferableCatalogue get() {
            return new OfferableCatalogue(catalogue.jobIds(), catalogue.toolIds());
        }

        @Override
        public void save(OfferableCatalogue saved) {
            catalogue = saved;
            catalogueSaves++;
        }
    };
    /** Homa's duration range as announced; empty before the first announcement. */
    public Optional<HoursRange> announcedHours = Optional.of(HOMA_HOURS);
    public final HomaHoursRepository homaHours = new HomaHoursRepository() {
        @Override
        public Optional<HoursRange> current() {
            return announcedHours;
        }

        @Override
        public void save(HoursRange homaRange) {
            announcedHours = Optional.of(homaRange);
        }
    };

    /** A provider with nothing set up yet. */
    public String newProvider() {
        offerings.save(Offering.startFor(OfferingFixtures.PROVIDER));
        return OfferingFixtures.PROVIDER;
    }

    /** A provider with everything set up, two helpers and a supervisor, not live yet. */
    public String readyProvider() {
        offerings.save(OfferingFixtures.readyToGoLive());
        return OfferingFixtures.PROVIDER;
    }

    public String liveProvider() {
        offerings.save(OfferingFixtures.live());
        return OfferingFixtures.PROVIDER;
    }

    public Offering offering() {
        return offerings.find(OfferingFixtures.PROVIDER).orElseThrow();
    }
}
