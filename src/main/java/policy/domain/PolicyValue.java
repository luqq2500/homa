package policy.domain;

import kernel.Percentage;
import policy.domain.exception.CheckInRadiusNotPositiveException;
import java.time.Duration;
import java.util.Objects;

/**
 * One Homa-set value. Homa changes the policy one value at a time, and each change makes a new
 * version. Each value checks its own shape; rules across values are checked by {@link HomaPolicy}.
 */
public sealed interface PolicyValue {

    /** Which value this is. A new value with the same key replaces the old one. */
    default Object key() {
        return getClass();
    }

    // ---- Pricing ----

    /** Homa's service fee: a share of the provider price, paid by the customer. */
    record ServiceFee(Percentage ofProviderPrice) implements PolicyValue {
        public ServiceFee {
            Objects.requireNonNull(ofProviderPrice);
        }
    }

    // ---- Search ----

    /** How long a booking can be, in whole hours. Each provider sets their own range within it. */
    record DurationRange(int minHours, int maxHours) implements PolicyValue {
        public DurationRange {
            PolicyShape.requireRange("The duration range", minHours, maxHours);
        }
    }

    /** How many helpers a customer can search for. */
    record HelpersPerSearch(int min, int max) implements PolicyValue {
        public HelpersPerSearch {
            PolicyShape.requireRange("Helpers per search", min, max);
        }
    }

    /** How soon a booking can start: at least this long from now. */
    record MinimumNotice(Duration period) implements PolicyValue {
        public MinimumNotice {
            PolicyShape.requirePeriod("The minimum notice", period);
        }
    }

    /** How far ahead a booking can start. */
    record BookingHorizon(Duration period) implements PolicyValue {
        public BookingHorizon {
            PolicyShape.requirePeriod("The booking horizon", period);
        }
    }

    // ---- Requests ----

    /** How long the provider has to answer a request, unless the booked start comes sooner. */
    record RequestExpiry(Duration period) implements PolicyValue {
        public RequestExpiry {
            PolicyShape.requirePeriod("The request expiry time", period);
        }
    }

    // ---- Cancellation ----

    /** How long before the booked start the Standard and Firm fees apply. */
    record FeeWindow(Duration period) implements PolicyValue {
        public FeeWindow {
            PolicyShape.requirePeriod("The fee window", period);
        }
    }

    /** The share of the hours fee a tier charges when the customer cancels within the fee window. */
    record CancellationTierFee(CancellationTier tier, Percentage ofHoursFee) implements PolicyValue {
        public CancellationTierFee {
            Objects.requireNonNull(tier);
            Objects.requireNonNull(ofHoursFee);
        }

        @Override
        public Object key() {
            return tier;
        }
    }

    /** The fine for a provider who cancels within the fee window before the booked start. */
    record ProviderLateCancellationFine(Percentage ofHoursFee) implements PolicyValue {
        public ProviderLateCancellationFine {
            Objects.requireNonNull(ofHoursFee);
        }
    }

    /** The fine for a provider no-show, or a provider cancellation after the booked start or check-in. */
    record ProviderNoShowFine(Percentage ofHoursFee) implements PolicyValue {
        public ProviderNoShowFine {
            Objects.requireNonNull(ofHoursFee);
        }
    }

    /** The most of a payout that can go to outstanding fines. */
    record FineDeductionCap(Percentage ofPayout) implements PolicyValue {
        public FineDeductionCap {
            Objects.requireNonNull(ofPayout);
        }
    }

    // ---- Check-in ----

    /** How long before the booked start the provider can check in. */
    record EarliestCheckIn(Duration beforeStart) implements PolicyValue {
        public EarliestCheckIn {
            PolicyShape.requirePeriod("The earliest check-in", beforeStart);
        }
    }

    /** How long after the booked start the provider can still check in. */
    record CheckInPeriod(Duration afterStart) implements PolicyValue {
        public CheckInPeriod {
            PolicyShape.requirePeriod("The check-in period", afterStart);
        }
    }

    /** How close the device must be to the customer's location to check in. */
    record CheckInRadius(int metres) implements PolicyValue {
        public CheckInRadius {
            if (metres < 1) {
                throw new CheckInRadiusNotPositiveException(metres);
            }
        }
    }

    // ---- Customer no-show ----

    /** How long the provider waits after the booked start or check-in before reporting a customer no-show. */
    record CustomerNoShowWait(Duration period) implements PolicyValue {
        public CustomerNoShowWait {
            PolicyShape.requirePeriod("The customer no-show wait", period);
        }
    }

    /** The customer no-show fee: the travel fee plus this share of the hours fee. */
    record CustomerNoShowFee(Percentage ofHoursFee) implements PolicyValue {
        public CustomerNoShowFee {
            Objects.requireNonNull(ofHoursFee);
        }
    }

    /** How long after a customer no-show report the customer can challenge it. */
    record NoShowReportWindow(Duration period) implements PolicyValue {
        public NoShowReportWindow {
            PolicyShape.requirePeriod("The no-show report window", period);
        }
    }

    // ---- Completion and disputes ----

    /** How long after the booked end Homa reminds the provider to submit completion. */
    record CompletionReminder(Duration afterEnd) implements PolicyValue {
        public CompletionReminder {
            PolicyShape.requirePeriod("The completion reminder time", afterEnd);
        }
    }

    /** How long after the booked end the provider has to submit completion before Homa reviews it. */
    record CompletionDeadline(Duration afterEnd) implements PolicyValue {
        public CompletionDeadline {
            PolicyShape.requirePeriod("The completion deadline", afterEnd);
        }
    }

    /** How long the dispute window runs after the later of the booked end and job completion. */
    record DisputeWindowExtension(Duration period) implements PolicyValue {
        public DisputeWindowExtension {
            PolicyShape.requirePeriod("The dispute window extension", period);
        }
    }

    /** How long evidence is kept after the reservation is resolved. */
    record EvidenceRetention(Duration period) implements PolicyValue {
        public EvidenceRetention {
            PolicyShape.requirePeriod("The evidence retention period", period);
        }
    }
}
