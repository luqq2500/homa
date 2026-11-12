package policy.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static policy.usecase.PolicyUsecaseFixtures.FOUNDER;
import static policy.usecase.PolicyUsecaseFixtures.LAUNCH;
import static policy.usecase.PolicyUsecaseFixtures.at;

import org.junit.jupiter.api.Test;
import policy.usecase.PolicyUsecaseFixtures.AuditLog;
import policy.usecase.PolicyUsecaseFixtures.InMemoryPolicies;
import policy.usecase.PolicyUsecaseFixtures.PublishedEvents;
import policy.usecase.dto.HomaViewPolicyResponse;
import policy.usecase.dto.HomaViewPolicyResponse.Value;
import policy.usecase.dto.LaunchPolicyRequest;

/** Homa's founders see every Homa-set value in force, by the name and in the form the change form takes. */
class HomaPolicyViewTest {

    @Test
    void theFoundersSeeEveryLaunchValueInTheOrderTheSpecListsThem() {
        InMemoryPolicies policies = new InMemoryPolicies();
        new HomaLaunchPolicy(policies, new PublishedEvents(), new AuditLog(), at(LAUNCH)).launch(new LaunchPolicyRequest(FOUNDER));

        HomaViewPolicyResponse inForce = new HomaViewPolicy(policies).view();

        assertThat(inForce.version()).isEqualTo(1);
        assertThat(inForce.effectiveFrom()).isEqualTo(LAUNCH);
        assertThat(inForce.values()).containsExactly(
                new Value("SERVICE_FEE", "PERCENTAGE", "15%"),
                new Value("DURATION_RANGE", "RANGE", "1-6"),
                new Value("MINIMUM_NOTICE", "PERIOD", "PT2H"),
                new Value("BOOKING_HORIZON", "PERIOD", "P30D"),
                new Value("REQUEST_EXPIRY", "PERIOD", "P1D"),
                new Value("FEE_WINDOW", "PERIOD", "P1D"),
                new Value("FLEXIBLE_CANCELLATION_FEE", "PERCENTAGE", "0%"),
                new Value("STANDARD_CANCELLATION_FEE", "PERCENTAGE", "10%"),
                new Value("FIRM_CANCELLATION_FEE", "PERCENTAGE", "20%"),
                new Value("PROVIDER_LATE_CANCELLATION_FINE", "PERCENTAGE", "20%"),
                new Value("PROVIDER_NO_SHOW_FINE", "PERCENTAGE", "30%"),
                new Value("FINE_DEDUCTION_CAP", "PERCENTAGE", "50%"),
                new Value("TEAM_REMINDER", "PERIOD", "PT6H"),
                new Value("EARLIEST_CHECK_IN", "PERIOD", "PT15M"),
                new Value("CHECK_IN_PERIOD", "PERIOD", "PT15M"),
                new Value("CHECK_IN_RADIUS", "COUNT", "300"),
                new Value("CUSTOMER_NO_SHOW_WAIT", "PERIOD", "PT15M"),
                new Value("CUSTOMER_NO_SHOW_FEE", "PERCENTAGE", "30%"),
                new Value("NO_SHOW_REPORT_WINDOW", "PERIOD", "PT1H"),
                new Value("COMPLETION_REMINDER", "PERIOD", "PT30M"),
                new Value("COMPLETION_DEADLINE", "PERIOD", "P1D"),
                new Value("DISPUTE_WINDOW_EXTENSION", "PERIOD", "PT1H"),
                new Value("EVIDENCE_RETENTION", "PERIOD", "P7D"),
                new Value("RATING_WINDOW", "PERIOD", "P14D"),
                new Value("MINIMUM_RATINGS_TO_SHOW", "COUNT", "3"));
        Value first = inForce.values().get(0);
        assertThat(first.name()).isEqualTo("SERVICE_FEE");
        assertThat(first.kind()).isEqualTo("PERCENTAGE");
        assertThat(first.amount()).isEqualTo("15%");
    }
}
