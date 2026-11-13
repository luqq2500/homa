package verification;

import org.junit.jupiter.api.Test;
import support.ArchitectureRules;

class VerificationArchitectureTest {

    @Test
    void domainDependsOnlyOnTheKernel() {
        ArchitectureRules.domainDependsOnlyOnKernel("verification");
    }

    @Test
    void usecaseDependsOnlyOnItsDomainAndTheSharedModules() {
        ArchitectureRules.usecaseDependsOnlyOnDomainAndShared("verification");
    }
}
