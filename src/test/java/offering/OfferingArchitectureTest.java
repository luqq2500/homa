package offering;

import org.junit.jupiter.api.Test;
import support.ArchitectureRules;

class OfferingArchitectureTest {

    @Test
    void domainDependsOnlyOnTheKernel() {
        ArchitectureRules.domainDependsOnlyOnKernel("offering");
    }

    @Test
    void usecaseDependsOnlyOnItsDomainAndTheSharedModules() {
        ArchitectureRules.usecaseDependsOnlyOnDomainAndShared("offering");
    }
}
