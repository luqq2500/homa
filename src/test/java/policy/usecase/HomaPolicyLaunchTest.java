package policy.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static policy.usecase.PolicyUsecaseFixtures.FOUNDER;
import static policy.usecase.PolicyUsecaseFixtures.LAUNCH;
import static policy.usecase.PolicyUsecaseFixtures.at;

import ie.events.HomaPolicyChangedIE;
import java.time.Duration;
import kernel.Percentage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import policy.usecase.PolicyUsecaseFixtures.AuditLog;
import policy.usecase.PolicyUsecaseFixtures.InMemoryPolicies;
import policy.usecase.PolicyUsecaseFixtures.PublishedEvents;
import policy.usecase.dto.LaunchPolicyRequest;
import policy.usecase.dto.LaunchPolicyResponse;
import policy.usecase.exception.DecidedByRequiredException;
import policy.usecase.exception.PolicyAlreadyLaunchedException;
import policy.usecase.port.out.AdminAction;

/** Homa's founders put the launch values in force, once. */
class HomaPolicyLaunchTest {

    private final InMemoryPolicies policies = new InMemoryPolicies();
    private final PublishedEvents events = new PublishedEvents();
    private final AuditLog auditLog = new AuditLog();
    private final HomaLaunchPolicy launch = new HomaLaunchPolicy(policies, events, auditLog, at(LAUNCH));

    @Test
    void theLaunchValuesAreSavedAsVersion1InForceFromNow() {
        LaunchPolicyResponse launched = launch.launch(new LaunchPolicyRequest(FOUNDER));

        assertThat(launched).isEqualTo(new LaunchPolicyResponse(1, LAUNCH));
        assertThat(launched.version()).isEqualTo(1);
        assertThat(launched.effectiveFrom()).isEqualTo(LAUNCH);
        assertThat(policies.versions).hasSize(1);
        assertThat(policies.findCurrent().orElseThrow().version()).isEqualTo(1);
    }

    @Test
    void theOtherContextsAreToldEveryLaunchValue() {
        launch.launch(new LaunchPolicyRequest(FOUNDER));

        assertThat(events.all).containsExactly(new HomaPolicyChangedIE(
                1, LAUNCH, Percentage.of(15), 1, 6,
                Duration.ofHours(2), Duration.ofDays(30), Duration.ofHours(24), Duration.ofHours(1), Duration.ofHours(24),
                Percentage.ZERO, Percentage.of(10), Percentage.of(20),
                Percentage.of(20), Percentage.of(30), Percentage.of(50),
                Duration.ofHours(6), Duration.ofMinutes(15), Duration.ofMinutes(15), 300,
                Duration.ofMinutes(15), Percentage.of(30), Duration.ofHours(1),
                Duration.ofMinutes(30), Duration.ofHours(24), Duration.ofHours(24), Duration.ofDays(120),
                Duration.ofDays(14), 3, LAUNCH));
    }

    @Test
    void theLaunchIsRecordedWithWhoLaunchedItAndWhen() {
        launch.launch(new LaunchPolicyRequest(FOUNDER));

        assertThat(auditLog.all).containsExactly(new AdminAction(FOUNDER, "LAUNCH_POLICY", "version 1", "", LAUNCH));
    }

    @Test
    void thePolicyIsLaunchedOnlyOnce() {
        launch.launch(new LaunchPolicyRequest(FOUNDER));

        assertThatThrownBy(() -> launch.launch(new LaunchPolicyRequest(FOUNDER)))
                .isInstanceOf(PolicyAlreadyLaunchedException.class)
                .hasMessage("The policy is already launched; version 1 is in force");

        assertThat(policies.versions).hasSize(1);
        assertThat(events.all).hasSize(1);
        assertThat(auditLog.all).hasSize(1);
    }

    @ParameterizedTest(name = "decided by \"{0}\"")
    @NullSource
    @ValueSource(strings = {"", "  "})
    void launchingNeedsTheFounderWhoDecidedIt(String decidedBy) {
        assertThatThrownBy(() -> launch.launch(new LaunchPolicyRequest(decidedBy)))
                .isInstanceOf(DecidedByRequiredException.class)
                .hasMessage("An admin action needs the founder who decided it");

        assertThat(policies.versions).isEmpty();
        assertThat(events.all).isEmpty();
        assertThat(auditLog.all).isEmpty();
    }

    @Test
    void theFoundersIdIsRecordedWithoutSpacesAroundIt() {
        launch.launch(new LaunchPolicyRequest(" " + FOUNDER + "  "));

        assertThat(auditLog.all.get(0).decidedBy()).isEqualTo(FOUNDER);
    }
}
