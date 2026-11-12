package policy.usecase;

import ie.port.PolicyIE;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import policy.domain.HomaPolicy;
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

    /** Keeps every version saved, the last one in force. */
    static final class InMemoryPolicies implements PolicyRepository {
        final List<HomaPolicy> versions = new ArrayList<>();

        @Override
        public Optional<HomaPolicy> findCurrent() {
            return versions.isEmpty() ? Optional.empty() : Optional.of(versions.get(versions.size() - 1));
        }

        @Override
        public void save(HomaPolicy policy) {
            versions.add(policy);
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
