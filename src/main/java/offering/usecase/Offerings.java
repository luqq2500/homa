package offering.usecase;

import ie.events.OfferingChangedIE;
import ie.events.ProviderListingStoppedIE;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Function;
import offering.domain.HelperRange;
import offering.domain.HoursRange;
import offering.domain.InsuranceDeclaration;
import offering.domain.Listing;
import offering.domain.Offering;
import offering.domain.OfferingState;
import offering.domain.ProviderProfile;
import offering.domain.TravelFee;
import offering.usecase.exception.OfferingNotFoundException;
import offering.usecase.exception.UnknownChoiceException;
import offering.usecase.port.out.OfferingIEPublisher;
import offering.usecase.port.out.OfferingRepository;

/** What the offering usecases share: lookups, reading choices, and saving a change and announcing it. */
final class Offerings {

    private Offerings() {
    }

    static Offering offering(OfferingRepository offerings, String providerId) {
        return offerings.find(providerId).orElseThrow(() -> new OfferingNotFoundException(providerId));
    }

    static <E extends Enum<E>> E choice(Class<E> options, String name) {
        try {
            return Enum.valueOf(options, name);
        } catch (IllegalArgumentException | NullPointerException unknown) {
            throw new UnknownChoiceException(name);
        }
    }

    /**
     * Saves the offering and tells the other contexts how it now stands; a provider who was live and
     * no longer is, is announced as stopped. Returns the listing.
     */
    static String saveAndAnnounce(Offering offering, Listing before, OfferingRepository offerings,
                                  OfferingIEPublisher publisher, Instant now) {
        offerings.save(offering);
        OfferingState state = offering.state();
        publisher.publish(changed(state, now));
        if (before == Listing.LIVE && state.listing() != Listing.LIVE) {
            publisher.publish(new ProviderListingStoppedIE(state.providerId(), state.stoppedBecause().name(), now));
        }
        return state.listing().name();
    }

    static OfferingChangedIE changed(OfferingState state, Instant now) {
        Optional<ProviderProfile> profile = Optional.ofNullable(state.profile());
        Optional<HelperRange> helpers = Optional.ofNullable(state.helpers());
        Optional<TravelFee> travelFee = Optional.ofNullable(state.travelFee());
        Optional<HoursRange> hours = Optional.ofNullable(state.hours());
        Optional<InsuranceDeclaration> insurance = Optional.ofNullable(state.insurance());
        return new OfferingChangedIE(state.providerId(), state.listing().name(),
                profile.map(ProviderProfile::name).orElse(null), profile.map(ProviderProfile::picture).orElse(null),
                state.jobIds(), state.toolIds(),
                helpers.map(HelperRange::min).orElse(0), helpers.map(HelperRange::max).orElse(0),
                state.supervisorRule().fromHelpers(), state.supervisorRule().fromHours(), state.hourlyRate(),
                travelFee.map(TravelFee::separate).orElse(false), travelFee.map(TravelFee::amount).orElse(null),
                hours.map(HoursRange::min).orElse(0), hours.map(HoursRange::max).orElse(0), state.cities(),
                name(state.cancellationTier()), name(state.acceptancePolicy()),
                insurance.map(InsuranceDeclaration::insurer).orElse(null),
                insurance.map(InsuranceDeclaration::validUntil).orElse(null), now);
    }

    static String name(Enum<?> choice) {
        return Optional.ofNullable(choice).map((Function<Enum<?>, String>) Enum::name).orElse(null);
    }
}
