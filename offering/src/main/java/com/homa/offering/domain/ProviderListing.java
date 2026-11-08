package com.homa.offering.domain;

import com.homa.offering.domain.exception.CityNotInServiceAreaException;
import com.homa.offering.domain.exception.HelpersExceedTeamSizeException;
import com.homa.offering.domain.exception.HelpersOutOfRangeException;
import com.homa.offering.domain.exception.HoursOutOfRangeException;
import com.homa.offering.domain.exception.JobNotInCatalogueException;
import com.homa.offering.domain.exception.ListingAlreadyLiveException;
import com.homa.offering.domain.exception.ListingNotLiveException;
import com.homa.offering.domain.exception.ListingNotReadyException;
import com.homa.offering.domain.exception.NoCitiesCoveredException;
import com.homa.offering.domain.exception.NoJobsOfferedException;
import com.homa.offering.domain.exception.ToolNotInCatalogueException;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BooleanSupplier;

/**
 * A provider's listing: what they sell, at what price, when and where. A listing starts as a draft and
 * goes live once it is complete and the provider and their helpers are verified.
 *
 * <p>A live listing can still change; each change affects new requests only, because a request keeps the
 * {@link OfferingSnapshot} it was made with.
 */
public final class ProviderListing {

    private final String id;
    private final String providerId;
    private Profile profile;
    private Set<JobCode> jobs = Set.of();
    private Set<ToolCode> tools = Set.of();
    private HourlyRate hourlyRate;
    private TravelFee travelFee;
    private HoursRange hours;
    private HelpersRange helpers;
    private Set<City> cities = Set.of();
    private CancellationTier cancellationTier;
    private TeamRoster roster = TeamRoster.empty();
    private boolean live;

    private ProviderListing(String id, String providerId) {
        this.id = id;
        this.providerId = providerId;
    }

    public static ProviderListing draft(String providerId) {
        return new ProviderListing(new ProviderListingId().generate(), OfferingText.require(providerId, "A provider id"));
    }

    public void setProfile(Profile newProfile) {
        profile = Objects.requireNonNull(newProfile);
    }

    /** Offers {@code offered}, every one an active job on Homa's job list. */
    public void offerJobs(Set<JobCode> offered, Set<JobCode> activeCatalogueJobs) {
        if (offered.isEmpty()) {
            throw new NoJobsOfferedException();
        }
        offered.stream()
                .filter(job -> !activeCatalogueJobs.contains(job))
                .findFirst()
                .ifPresent(job -> {
                    throw new JobNotInCatalogueException(job);
                });
        jobs = Set.copyOf(offered);
    }

    /** Says which tools the provider brings, every one an active tool on Homa's tool list. Bringing none is allowed. */
    public void bringTools(Set<ToolCode> brought, Set<ToolCode> activeCatalogueTools) {
        brought.stream()
                .filter(tool -> !activeCatalogueTools.contains(tool))
                .findFirst()
                .ifPresent(tool -> {
                    throw new ToolNotInCatalogueException(tool);
                });
        tools = Set.copyOf(brought);
    }

    public void setPricing(HourlyRate rate, TravelFee travel) {
        hourlyRate = Objects.requireNonNull(rate);
        travelFee = Objects.requireNonNull(travel);
    }

    /** Sets the hours a customer can book, within Homa's duration range. */
    public void setHours(HoursRange range, HomaLimits limits) {
        if (range.min() < limits.minHours() || range.max() > limits.maxHours()) {
            throw new HoursOutOfRangeException(range.min(), range.max(), limits.minHours(), limits.maxHours());
        }
        hours = range;
    }

    /** Sets the helpers a customer can book: within Homa's helpers per search and at most the verified team size. */
    public void setHelpers(HelpersRange range, HomaLimits limits) {
        if (range.min() < limits.minHelpers() || range.max() > limits.maxHelpers()) {
            throw new HelpersOutOfRangeException(range.min(), range.max(), limits.minHelpers(), limits.maxHelpers());
        }
        if (range.max() > roster.size()) {
            throw new HelpersExceedTeamSizeException(range.max(), roster.size());
        }
        helpers = range;
    }

    public void coverCities(Set<City> covered, ServiceAreaView area) {
        if (covered.isEmpty()) {
            throw new NoCitiesCoveredException();
        }
        covered.stream()
                .filter(city -> !area.covers(city))
                .findFirst()
                .ifPresent(city -> {
                    throw new CityNotInServiceAreaException(city);
                });
        cities = Set.copyOf(covered);
    }

    public void chooseCancellationTier(CancellationTier tier) {
        cancellationTier = Objects.requireNonNull(tier);
    }

    /**
     * Records the provider's verified team. If the team shrinks below the helpers range, the range
     * shrinks with it, so the provider can always name everyone who is coming. A live listing whose team
     * becomes empty is no longer live.
     */
    public void updateRoster(TeamRoster newRoster) {
        roster = Objects.requireNonNull(newRoster);
        if (helpers != null) {
            helpers = helpers.fittedTo(roster.size()).orElse(null);
        }
        live = live && helpers != null;
    }

    /**
     * Takes the listing live.
     *
     * @param providerReady whether the provider's own eKYC is verified and their payout account is ready
     * @param allHelpersVerified whether every helper on the team is verified
     */
    public void goLive(boolean providerReady, boolean allHelpersVerified) {
        if (live) {
            throw new ListingAlreadyLiveException();
        }
        Set<GoLiveRequirement> missing = EnumSet.noneOf(GoLiveRequirement.class);
        missing.addAll(missingForGoLive());
        addIfNot(providerReady, GoLiveRequirement.PROVIDER_VERIFIED_AND_PAYOUT_READY, missing);
        addIfNot(allHelpersVerified, GoLiveRequirement.ALL_HELPERS_VERIFIED, missing);
        if (!missing.isEmpty()) {
            throw new ListingNotReadyException(missing);
        }
        live = true;
    }

    /** What the listing itself still needs before it can go live. Verification is checked at go-live. */
    public Set<GoLiveRequirement> missingForGoLive() {
        Set<GoLiveRequirement> missing = EnumSet.noneOf(GoLiveRequirement.class);
        requirements().forEach((requirement, met) -> addIfNot(met.getAsBoolean(), requirement, missing));
        return Set.copyOf(missing);
    }

    /** What a reservation copies at request time. Only a live listing can be reserved. */
    public OfferingSnapshot snapshot() {
        if (!live) {
            throw new ListingNotLiveException();
        }
        return new OfferingSnapshot(
                providerId, hourlyRate, travelFee, hours, helpers, jobs, tools, cities, cancellationTier, roster);
    }

    public boolean isLive() {
        return live;
    }

    public String id() {
        return id;
    }

    public String providerId() {
        return providerId;
    }

    public Optional<Profile> profile() {
        return Optional.ofNullable(profile);
    }

    public Set<JobCode> jobs() {
        return jobs;
    }

    public Set<ToolCode> tools() {
        return tools;
    }

    public Optional<HourlyRate> hourlyRate() {
        return Optional.ofNullable(hourlyRate);
    }

    public Optional<TravelFee> travelFee() {
        return Optional.ofNullable(travelFee);
    }

    public Optional<HoursRange> hours() {
        return Optional.ofNullable(hours);
    }

    public Optional<HelpersRange> helpers() {
        return Optional.ofNullable(helpers);
    }

    public Set<City> cities() {
        return cities;
    }

    public Optional<CancellationTier> cancellationTier() {
        return Optional.ofNullable(cancellationTier);
    }

    public TeamRoster roster() {
        return roster;
    }

    private Map<GoLiveRequirement, BooleanSupplier> requirements() {
        Map<GoLiveRequirement, BooleanSupplier> requirements = new EnumMap<>(GoLiveRequirement.class);
        requirements.put(GoLiveRequirement.PROFILE, () -> profile != null);
        requirements.put(GoLiveRequirement.PROFILE_PICTURE, () -> profile != null && profile.hasPicture());
        requirements.put(GoLiveRequirement.JOBS, () -> !jobs.isEmpty());
        requirements.put(GoLiveRequirement.PRICING, () -> hourlyRate != null);
        requirements.put(GoLiveRequirement.HOURS, () -> hours != null);
        requirements.put(GoLiveRequirement.HELPERS, () -> helpers != null);
        requirements.put(GoLiveRequirement.CITIES, () -> !cities.isEmpty());
        requirements.put(GoLiveRequirement.CANCELLATION_TIER, () -> cancellationTier != null);
        return requirements;
    }

    private static void addIfNot(boolean met, GoLiveRequirement requirement, Set<GoLiveRequirement> missing) {
        if (!met) {
            missing.add(requirement);
        }
    }
}
