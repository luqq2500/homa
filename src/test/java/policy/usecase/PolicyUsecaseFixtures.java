package policy.usecase;

import ie.port.PolicyIE;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import policy.domain.HomaPolicy;
import policy.usecase.exception.ChangedMeanwhileException;
import policy.usecase.port.out.AdminAction;
import policy.usecase.port.out.AdminAuditLog;
import policy.usecase.port.out.PolicyIEPublisher;
import policy.usecase.port.out.PolicyRepository;

/** In-memory stand-ins for the policy's out ports, and the values the usecase tests share. */
final class PolicyUsecaseFixtures {

    static final Instant LAUNCH = Instant.parse("2026-11-01T00:00:00Z");
    static final Instant A_WEEK_LATER = Instant.parse("2026-11-08T00:00:00Z");
    static final String FOUNDER = "founder-aisyah";

    private PolicyUsecaseFixtures() {
    }

    static Clock at(Instant now) {
        return Clock.fixed(now, ZoneOffset.UTC);
    }

    /**
     * Keeps every version saved, the last one in force. Like a real repository, it refuses a version that is
     * not one more than the version in force.
     */
    static class InMemoryPolicies implements PolicyRepository {
        final List<HomaPolicy> versions = new ArrayList<>();

        @Override
        public Optional<HomaPolicy> findCurrent() {
            return versions.isEmpty() ? Optional.empty() : Optional.of(versions.get(versions.size() - 1));
        }

        @Override
        public void save(HomaPolicy policy) {
            if (policy.version() != versions.size() + 1) {
                throw new ChangedMeanwhileException("The Homa-set values");
            }
            versions.add(policy);
        }
    }

    /**
     * A repository where another founder's change lands between reading and saving: each founder reads the
     * version before the one in force.
     */
    static final class AnotherFounderChangesFirst extends InMemoryPolicies {

        @Override
        public Optional<HomaPolicy> findCurrent() {
            return versions.size() < 2 ? super.findCurrent() : Optional.of(versions.get(versions.size() - 2));
        }
    }

    static final class PublishedEvents implements PolicyIEPublisher {
        final List<PolicyIE> all = new ArrayList<>();

        @Override
        public void publish(PolicyIE event) {
            all.add(event);
        }
    }

    static final class AuditLog implements AdminAuditLog {
        final List<AdminAction> all = new ArrayList<>();

        @Override
        public void record(AdminAction action) {
            all.add(action);
        }
    }
}
