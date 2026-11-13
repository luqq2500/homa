package policy;

import org.junit.jupiter.api.Test;
import support.ArchitectureRules;

class PolicyArchitectureTest {

    @Test
    void domainDependsOnlyOnTheKernel() {
        ArchitectureRules.domainDependsOnlyOnKernel("policy");
    }

    @Test
    void usecaseDependsOnlyOnItsDomainAndTheSharedModules() {
        ArchitectureRules.usecaseDependsOnlyOnDomainAndShared("policy");
    }
}
