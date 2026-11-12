package offering.domain;

import offering.domain.exception.CityNotInServiceAreaException;
import offering.domain.exception.HelpersExceedTeamSizeException;
import offering.domain.exception.HoursOutOfRangeException;
import offering.domain.exception.JobNotInCatalogueException;
import offering.domain.exception.ListingAlreadyLiveException;
import offering.domain.exception.ListingAlreadyPausedException;
import offering.domain.exception.ListingNotLiveException;
import offering.domain.exception.ListingNotPausedException;
import offering.domain.exception.ListingNotReadyException;
import offering.domain.exception.NoCitiesCoveredException;
import offering.domain.exception.NoJobsOfferedException;
import offering.domain.exception.NoSupervisorOnTeamException;
import offering.domain.exception.ToolNotInCatalogueException;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

/**
 * A provider's listing: what they sell, at what price, when and where. A listing starts as a draft and
 * goes live once it is complete and the provider is verified.
 *
 * <p>A live listing can still change; each change affects new requests only, because a request keeps the
 * {@link OfferingSnapshot} it was made with.
 *
 * <p>A live listing is paused while the provider's re-verification has lapsed: it stays live, so accepted
 * reservations go ahead, but it takes no new requests and is left out of search.
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
    private SupervisorRule supervisorRule;
    private boolean live;
    private boolean paused;

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

    /**
     * Homa retired {@code retired} from its job list: the listing no longer offers it. A live listing left
     * with no job is no longer live until the provider offers one again. A request already made keeps the
     * offering it was made with.
     */
    public JobWithdrawal withdrawJob(JobCode retired) {
        if (!jobs.contains(retired)) {
            return JobWithdrawal.NOT_OFFERED;
        }
        jobs = without(jobs, retired);
        return jobs.isEmpty() ? goOfflineWithNoJob() : JobWithdrawal.WITHDRAWN;
    }

    private JobWithdrawal goOfflineWithNoJob() {
        return goOffline() ? JobWithdrawal.WITHDRAWN_AND_OFFLINE : JobWithdrawal.WITHDRAWN;
    }

    /** Takes the listing offline and returns whether it was live. */
    private boolean goOffline() {
        boolean wasLive = live;
        live = false;
        paused = false;
        return wasLive;
    }

    /**
     * Homa retired {@code retired} from its tool list: the listing no longer says the provider brings it.
     *
     * @return whether the provider brought the tool, so the listing changed
     */
    public boolean withdrawTool(ToolCode retired) {
        boolean brought = tools.contains(retired);
        tools = without(tools, retired);
        return brought;
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

    /**
     * Homa changed its duration range: the listing's hours keep inside it. Like the helper range, they only
     * ever shrink: a wider range changes nothing, and the provider sets their hours again to use it. When
     * none of the hours are inside, they are cleared, and a live listing is no longer live.
     */
    public HoursFit fitHours(HomaLimits limits) {
        if (hours == null) {
            return HoursFit.NOT_AFFECTED;
        }
        int min = Math.max(hours.min(), limits.minHours());
        int max = Math.min(hours.max(), limits.maxHours());
        if (min > max) {
            hours = null;
            return goOffline() ? HoursFit.CLEARED_AND_OFFLINE : HoursFit.CLEARED;
        }
        HoursRange fitted = HoursRange.of(min, max);
        if (fitted.equals(hours)) {
            return HoursFit.NOT_AFFECTED;
        }
        hours = fitted;
        return HoursFit.NARROWED;
    }

    /**
     * Sets the helpers a customer can book. Homa sets no upper limit; the maximum is at most the helpers on
     * the team, and supervisors never count towards it.
     */
    public void setHelpers(HelpersRange range) {
        if (range.max() > roster.helperCount()) {
            throw new HelpersExceedTeamSizeException(range.max(), roster.helperCount());
        }
        helpers = range;
    }

    /** Sets when a supervisor is sent. It needs a supervisor on the team. */
    public void setSupervisorRule(SupervisorRule rule) {
        Objects.requireNonNull(rule);
        if (!roster.hasSupervisor()) {
            throw new NoSupervisorOnTeamException();
        }
        supervisorRule = rule;
    }

    /** The provider no longer has to send a supervisor, though they still can. */
    public void clearSupervisorRule() {
        supervisorRule = null;
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
     * Records the provider's team. If the team's helpers drop below the helpers range, the range shrinks
     * with them, so the provider can always name everyone who is coming. A live listing with no helper
     * left, or with a supervisor rule and no supervisor left, is no longer live.
     */
    public void updateRoster(TeamRoster newRoster) {
        roster = Objects.requireNonNull(newRoster);
        fitHelpersToRoster();
        live = live && keepsItsTeamRequirements();
        paused = paused && live;
    }

    private void fitHelpersToRoster() {
        if (helpers != null) {
            helpers = helpers.fittedTo(roster.helperCount()).orElse(null);
        }
    }

    private boolean keepsItsTeamRequirements() {
        return helpers != null && hasSupervisorIfRequired();
    }

    private boolean hasSupervisorIfRequired() {
        return supervisorRule == null || roster.hasSupervisor();
    }

    /**
     * Takes the listing live. Only team members are on the roster, so people still waiting for
     * verification never block going live; the helper range set from the roster already covers the
     * minimum helpers.
     *
     * @param providerReady whether the business's KYB is verified and not lapsed, and its payout account
     *     is ready
     */
    public void goLive(boolean providerReady) {
        if (live) {
            throw new ListingAlreadyLiveException();
        }
        Set<GoLiveRequirement> missing = EnumSet.noneOf(GoLiveRequirement.class);
        missing.addAll(missingForGoLive());
        addIfNot(providerReady, GoLiveRequirement.PROVIDER_VERIFIED_AND_PAYOUT_READY, missing);
        if (!missing.isEmpty()) {
            throw new ListingNotReadyException(missing);
        }
        live = true;
    }

    /**
     * Pauses a live listing because the provider's re-verification lapsed. It stays live, so accepted
     * reservations go ahead, but it takes no new requests until it resumes.
     */
    public void pause() {
        if (!live) {
            throw new ListingNotLiveException();
        }
        if (paused) {
            throw new ListingAlreadyPausedException();
        }
        paused = true;
    }

    /** Resumes a paused listing once the provider passes re-verification. */
    public void resume() {
        if (!paused) {
            throw new ListingNotPausedException();
        }
        paused = false;
    }

    /** What the listing itself still needs before it can go live. Verification is checked at go-live. */
    public Set<GoLiveRequirement> missingForGoLive() {
        Set<GoLiveRequirement> missing = EnumSet.noneOf(GoLiveRequirement.class);
        requirements().forEach((requirement, met) -> addIfNot(met.getAsBoolean(), requirement, missing));
        return Set.copyOf(missing);
    }

    /**
     * What a reservation copies at request time. Only a live listing has one; a paused listing's snapshot
     * says it takes no requests.
     */
    public OfferingSnapshot snapshot() {
        if (!live) {
            throw new ListingNotLiveException();
        }
        return new OfferingSnapshot(
                providerId, hourlyRate, travelFee, hours, helpers, jobs, tools, cities, cancellationTier, roster,
                Optional.ofNullable(supervisorRule), !paused);
    }

    public boolean isLive() {
        return live;
    }

    public boolean isPaused() {
        return paused;
    }

    /** Whether the listing takes new requests: live and not paused. */
    public boolean takesRequests() {
        return live && !paused;
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

    public Optional<SupervisorRule> supervisorRule() {
        return Optional.ofNullable(supervisorRule);
    }

    private Map<GoLiveRequirement, BooleanSupplier> requirements() {
        Map<GoLiveRequirement, BooleanSupplier> requirements = new EnumMap<>(GoLiveRequirement.class);
        requirements.put(GoLiveRequirement.PROFILE, () -> profile != null);
        requirements.put(GoLiveRequirement.PROFILE_PICTURE, () -> profile != null && profile.hasPicture());
        requirements.put(GoLiveRequirement.JOBS, () -> !jobs.isEmpty());
        requirements.put(GoLiveRequirement.PRICING, () -> hourlyRate != null);
        requirements.put(GoLiveRequirement.HOURS, () -> hours != null);
        requirements.put(GoLiveRequirement.HELPERS, () -> helpers != null);
        requirements.put(GoLiveRequirement.SUPERVISOR, this::hasSupervisorIfRequired);
        requirements.put(GoLiveRequirement.CITIES, () -> !cities.isEmpty());
        requirements.put(GoLiveRequirement.CANCELLATION_TIER, () -> cancellationTier != null);
        return requirements;
    }

    private static <T> Set<T> without(Set<T> all, T gone) {
        return all.stream().filter(one -> !one.equals(gone)).collect(Collectors.toUnmodifiableSet());
    }

    private static void addIfNot(boolean met, GoLiveRequirement requirement, Set<GoLiveRequirement> missing) {
        if (!met) {
            missing.add(requirement);
        }
    }
}
