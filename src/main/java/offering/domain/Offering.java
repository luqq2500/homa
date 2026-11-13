package offering.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import kernel.City;
import kernel.Money;
import offering.domain.exception.InvalidHelperRangeException;
import offering.domain.exception.InvalidHourlyRateException;
import offering.domain.exception.InvalidHoursException;
import offering.domain.exception.NoCityCoveredException;
import offering.domain.exception.NoJobOfferedException;
import offering.domain.exception.NotReadyToGoLiveException;
import offering.domain.exception.SupervisorRuleNeedsSupervisorException;

/**
 * What a provider offers and on what terms, and whether they are live. The provider sets it up in
 * any order; Homa keeps it inside its rules as the team, the job list and Homa's ranges change. A
 * change applies to new requests only.
 */
public final class Offering {

    private final String providerId;
    private Listing listing;
    private StopReason stoppedBecause;
    private ProviderProfile profile;
    private Set<String> jobIds;
    private Set<String> toolIds;
    private HelperRange helpers;
    private SupervisorRule supervisorRule;
    private Money hourlyRate;
    private TravelFee travelFee;
    private HoursRange hours;
    private Set<City> cities;
    private CancellationTier cancellationTier;
    private AcceptancePolicy acceptancePolicy;
    private InsuranceDeclaration insurance;
    private boolean businessVerified;
    private boolean payoutAccountAdded;
    private Team team;

    private Offering(OfferingState state) {
        this.providerId = state.providerId();
        this.listing = state.listing();
        this.stoppedBecause = state.stoppedBecause();
        this.profile = state.profile();
        this.jobIds = state.jobIds();
        this.toolIds = state.toolIds();
        this.helpers = state.helpers();
        this.supervisorRule = state.supervisorRule();
        this.hourlyRate = state.hourlyRate();
        this.travelFee = state.travelFee();
        this.hours = state.hours();
        this.cities = state.cities();
        this.cancellationTier = state.cancellationTier();
        this.acceptancePolicy = state.acceptancePolicy();
        this.insurance = state.insurance();
        this.businessVerified = state.businessVerified();
        this.payoutAccountAdded = state.payoutAccountAdded();
        this.team = state.team();
    }

    /** A new business's offering: nothing set, Request and accept by default, not live. */
    public static Offering startFor(String providerId) {
        return new Offering(new OfferingState(providerId, Listing.DRAFT, null, null, Set.of(), Set.of(), null,
                SupervisorRule.NEVER, null, null, null, Set.of(), null, AcceptancePolicy.REQUEST_AND_ACCEPT, null,
                false, false, Team.EMPTY));
    }

    public static Offering restore(OfferingState state) {
        return new Offering(state);
    }

    public OfferingState state() {
        return new OfferingState(providerId, listing, stoppedBecause, profile, jobIds, toolIds, helpers,
                supervisorRule, hourlyRate, travelFee, hours, cities, cancellationTier, acceptancePolicy, insurance,
                businessVerified, payoutAccountAdded, team);
    }

    // ---- What the provider sets ----

    public void setProfile(String name, String picture) {
        this.profile = new ProviderProfile(name, picture);
    }

    /** The jobs offered, from Homa's job list: at least one. */
    public void offerJobs(Set<String> chosenJobIds, OfferableCatalogue catalogue) {
        if (chosenJobIds.isEmpty()) {
            throw new NoJobOfferedException();
        }
        catalogue.requireOfferable(chosenJobIds);
        this.jobIds = Set.copyOf(chosenJobIds);
    }

    /** The tools brought, from Homa's tool list. Bringing no tools is allowed. */
    public void bringTools(Set<String> chosenToolIds, OfferableCatalogue catalogue) {
        catalogue.requirePickable(chosenToolIds);
        this.toolIds = Set.copyOf(chosenToolIds);
    }

    /** The maximum cannot be more than the helpers on the team, so the provider can always name everyone coming. */
    public void setHelperRange(int min, int max) {
        HelperRange range = new HelperRange(min, max);
        if (max > team.helpers()) {
            throw new InvalidHelperRangeException(
                    "The maximum helpers cannot be more than the " + team.helpers() + " helpers on the team");
        }
        this.helpers = range;
    }

    /** A provider with supervisors can set when one is sent; clearing the rule is always allowed. */
    public void setSupervisorRule(int fromHelpers, int fromHours) {
        SupervisorRule rule = new SupervisorRule(fromHelpers, fromHours);
        if (rule.isSet() && team.supervisors() < 1) {
            throw new SupervisorRuleNeedsSupervisorException();
        }
        this.supervisorRule = rule;
    }

    public void setHourlyRate(Money hourlyRate) {
        if (hourlyRate == null || hourlyRate.isZero()) {
            throw new InvalidHourlyRateException();
        }
        this.hourlyRate = hourlyRate;
    }

    public void setTravelFee(TravelFee travelFee) {
        this.travelFee = travelFee;
    }

    /** The provider's own hours, inside the range Homa sets. */
    public void setHours(int min, int max, HoursRange homaRange) {
        HoursRange range = new HoursRange(min, max);
        if (!range.isWithin(homaRange)) {
            throw new InvalidHoursException(
                    "Hours are within Homa's range of " + homaRange.min() + " to " + homaRange.max() + " hours");
        }
        this.hours = range;
    }

    public void coverCities(Set<City> cities) {
        if (cities.isEmpty()) {
            throw new NoCityCoveredException();
        }
        this.cities = Set.copyOf(cities);
    }

    public void chooseCancellationTier(CancellationTier tier) {
        this.cancellationTier = tier;
    }

    public void chooseAcceptancePolicy(AcceptancePolicy policy) {
        this.acceptancePolicy = policy;
    }

    public void declareInsurance(String insurer, Instant validUntil) {
        this.insurance = new InsuranceDeclaration(insurer, validUntil);
    }

    /** Goes live when nothing is missing; otherwise fails with everything still missing. */
    public void goLive() {
        List<Missing> missing = missingToGoLive();
        if (!missing.isEmpty()) {
            throw new NotReadyToGoLiveException(missing.stream().map(Missing::name).toList());
        }
        listing = Listing.LIVE;
        stoppedBecause = null;
    }

    /** Everything still missing before the provider can go live. */
    public List<Missing> missingToGoLive() {
        List<Missing> missing = new ArrayList<>();
        addIf(missing, !businessVerified, Missing.BUSINESS_VERIFIED);
        addIf(missing, profile == null, Missing.PROFILE);
        addIf(missing, jobIds.isEmpty(), Missing.JOBS);
        addIf(missing, cities.isEmpty(), Missing.CITIES);
        addMissingTerms(missing);
        addIf(missing, cancellationTier == null, Missing.CANCELLATION_POLICY);
        addIf(missing, !payoutAccountAdded, Missing.PAYOUT_ACCOUNT);
        addIf(missing, supervisorMissing(), Missing.SUPERVISOR);
        return List.copyOf(missing);
    }

    private void addMissingTerms(List<Missing> missing) {
        addIf(missing, hourlyRate == null, Missing.HOURLY_RATE);
        addIf(missing, travelFee == null, Missing.TRAVEL_FEE);
        addIf(missing, hours == null, Missing.HOURS);
        addIf(missing, helpers == null, Missing.HELPERS);
    }

    private static void addIf(List<Missing> missing, boolean isMissing, Missing what) {
        if (isMissing) {
            missing.add(what);
        }
    }

    private boolean supervisorMissing() {
        return supervisorRule.isSet() && team.supervisors() < 1;
    }

    // ---- What Homa adjusts ----

    /** A retired job comes off the listing; a live provider left with no job is taken offline. Returns whether it was offered. */
    public boolean retireJob(String jobId) {
        if (!jobIds.contains(jobId)) {
            return false;
        }
        Set<String> left = new HashSet<>(jobIds);
        left.remove(jobId);
        jobIds = Set.copyOf(left);
        stopIf(jobIds.isEmpty(), StopReason.NO_JOB);
        return true;
    }

    /** A retired tool comes off the listing. Returns whether it was brought. */
    public boolean retireTool(String toolId) {
        if (!toolIds.contains(toolId)) {
            return false;
        }
        Set<String> left = new HashSet<>(toolIds);
        left.remove(toolId);
        toolIds = Set.copyOf(left);
        return true;
    }

    /**
     * Homa changed its duration range: the provider's hours shrink to fit inside it, and a live
     * provider left with no hours goes offline. A wider range widens nothing. Returns whether the hours changed.
     */
    public boolean fitHoursInto(HoursRange homaRange) {
        if (hours == null || hours.isWithin(homaRange)) {
            return false;
        }
        hours = hours.fittedInto(homaRange).orElse(null);
        stopIf(hours == null, StopReason.NO_HOURS);
        return true;
    }

    /** A new or restored team member. A growing team does not widen the helper range; the provider sets it again. */
    public void teamMemberJoined(String memberId, TeamRole role) {
        team = team.with(memberId, role);
    }

    /** A change of role: a helper who becomes a supervisor lowers the helpers on the team. */
    public void teamMemberChangedRole(String memberId, TeamRole role) {
        team = team.with(memberId, role);
        keepWithinTeam();
    }

    public void teamMemberLeft(String memberId) {
        team = team.without(memberId);
        keepWithinTeam();
    }

    /** The helper range shrinks with the team; with no helper, or no supervisor for the rule, a live provider stops. */
    private void keepWithinTeam() {
        helpers = Optional.ofNullable(helpers).flatMap(range -> range.shrunkTo(team.helpers())).orElse(null);
        stopIf(team.helpers() < 1, StopReason.NO_HELPER);
        stopIf(supervisorMissing(), StopReason.NO_SUPERVISOR);
    }

    /** The business passed its KYB or a re-verification: a paused listing resumes by itself. */
    public void businessVerified() {
        businessVerified = true;
        if (listing == Listing.PAUSED) {
            listing = Listing.LIVE;
            stoppedBecause = null;
        }
    }

    /** The business lapsed or was seriously flagged: a live listing is paused and takes no new requests. */
    public void businessLapsed() {
        businessVerified = false;
        if (listing == Listing.LIVE) {
            listing = Listing.PAUSED;
            stoppedBecause = StopReason.BUSINESS_LAPSED;
        }
    }

    public void payoutAccountAdded() {
        payoutAccountAdded = true;
    }

    private void stopIf(boolean cannotStayLive, StopReason reason) {
        if (cannotStayLive && listing == Listing.LIVE) {
            listing = Listing.OFFLINE;
            stoppedBecause = reason;
        }
    }

    // ---- What others read ----

    public String providerId() {
        return providerId;
    }

    public Listing listing() {
        return listing;
    }

    public boolean isLive() {
        return listing == Listing.LIVE;
    }

    public Set<String> jobIds() {
        return jobIds;
    }

    public Set<String> toolIds() {
        return toolIds;
    }

    /** Whether the Insured badge shows: insurance was declared and is still valid. */
    public boolean isInsuredAt(Instant now) {
        return insurance != null && insurance.isValidAt(now);
    }
}
