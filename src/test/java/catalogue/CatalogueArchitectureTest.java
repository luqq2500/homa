package catalogue;

import org.junit.jupiter.api.Test;
import support.ArchitectureRules;

class CatalogueArchitectureTest {

    @Test
    void domainDependsOnlyOnTheKernel() {
        ArchitectureRules.domainDependsOnlyOnKernel("catalogue");
    }

    @Test
    void usecaseDependsOnlyOnItsDomainAndTheSharedModules() {
        ArchitectureRules.usecaseDependsOnlyOnDomainAndShared("catalogue");
    }
}
