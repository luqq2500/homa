package offering.support;

import java.time.Instant;
import java.util.Set;
import kernel.City;
import kernel.Money;
import offering.domain.CancellationTier;
import offering.domain.HoursRange;
import offering.domain.OfferableCatalogue;
import offering.domain.Offering;
import offering.domain.TeamRole;
import offering.domain.TravelFee;

/** Ready-made offerings for tests. */
public final class OfferingFixtures {

    public static final Instant NOW = Instant.parse("2026-03-07T02:00:00Z");
    public static final String PROVIDER = "provider-1";
    public static final HoursRange HOMA_HOURS = new HoursRange(1, 6);
    public static final String KITCHEN = "job-kitchen";
    public static final String LAUNDRY = "job-laundry";
    public static final String VACUUM = "tool-vacuum";
    public static final String MOP = "tool-mop";

    private OfferingFixtures() {
    }

    public static OfferableCatalogue catalogue() {
        return new OfferableCatalogue(Set.of(KITCHEN, LAUNDRY), Set.of(VACUUM, MOP));
    }

    /** Everything set up, with two helpers and a supervisor on the team, but not live yet. */
    public static Offering readyToGoLive() {
        Offering offering = Offering.startFor(PROVIDER);
        offering.businessVerified();
        offering.payoutAccountAdded();
        offering.teamMemberJoined("helper-1", TeamRole.HELPER);
        offering.teamMemberJoined("helper-2", TeamRole.HELPER);
        offering.teamMemberJoined("supervisor-1", TeamRole.SUPERVISOR);
        offering.setProfile("Bersih Home Care", "pictures/bersih.jpg");
        offering.offerJobs(Set.of(KITCHEN, LAUNDRY), catalogue());
        offering.bringTools(Set.of(VACUUM), catalogue());
        offering.setHelperRange(1, 2);
        offering.setHourlyRate(Money.ofRinggit(30));
        offering.setTravelFee(TravelFee.separate(Money.ofRinggit(10)));
        offering.setHours(2, 5, HOMA_HOURS);
        offering.coverCities(Set.of(City.CYBERJAYA, City.PUTRAJAYA));
        offering.chooseCancellationTier(CancellationTier.STANDARD);
        return offering;
    }

    public static Offering live() {
        Offering offering = readyToGoLive();
        offering.goLive();
        return offering;
    }
}
