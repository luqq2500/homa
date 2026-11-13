package policy.usecase;

import static policy.domain.HomaValue.*;

import ie.events.HomaValuesChangedIE;
import java.time.Instant;
import policy.domain.HomaValues;

/** Builds the policy context's integration events from its domain objects. */
final class HomaValuesEvents {

    private HomaValuesEvents() {
    }

    static HomaValuesChangedIE valuesChanged(HomaValues values, Instant occurredAt) {
        return new HomaValuesChangedIE(
                values.version(),
                values.percentageOf(SERVICE_FEE),
                (int) values.amountOf(MINIMUM_HOURS),
                (int) values.amountOf(MAXIMUM_HOURS),
                values.durationOf(MINIMUM_NOTICE),
                values.durationOf(BOOKING_HORIZON),
                values.durationOf(TEAM_REMINDER_TIME),
                values.durationOf(EARLIEST_CHECK_IN),
                values.durationOf(CHECK_IN_PERIOD),
                (int) values.amountOf(CHECK_IN_RADIUS),
                values.durationOf(CUSTOMER_NO_SHOW_WAIT),
                values.percentageOf(CUSTOMER_NO_SHOW_FEE),
                values.durationOf(NO_SHOW_REPORT_WINDOW),
                values.durationOf(REQUEST_EXPIRY_TIME),
                values.durationOf(RESPONSE_DEADLINE),
                values.durationOf(COMPLETION_REMINDER_TIME),
                values.durationOf(COMPLETION_DEADLINE),
                values.durationOf(EVIDENCE_RETENTION_PERIOD),
                values.durationOf(DISPUTE_WINDOW_EXTENSION),
                values.durationOf(FEE_WINDOW),
                values.percentageOf(STANDARD_CANCELLATION_FEE),
                values.percentageOf(FIRM_CANCELLATION_FEE),
                values.percentageOf(PROVIDER_LATE_CANCELLATION_FINE),
                values.percentageOf(PROVIDER_NO_SHOW_FINE),
                values.percentageOf(FINE_DEDUCTION_CAP),
                values.durationOf(RATING_WINDOW),
                (int) values.amountOf(MINIMUM_RATINGS_TO_SHOW),
                occurredAt);
    }
}
