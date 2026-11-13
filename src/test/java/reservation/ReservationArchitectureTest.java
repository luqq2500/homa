package reservation;

import org.junit.jupiter.api.Test;
import support.ArchitectureRules;

class ReservationArchitectureTest {

    @Test
    void domainDependsOnlyOnTheKernel() {
        ArchitectureRules.domainDependsOnlyOnKernel("reservation");
    }

    @Test
    void usecaseDependsOnlyOnItsDomainAndTheSharedModules() {
        ArchitectureRules.usecaseDependsOnlyOnDomainAndShared("reservation");
    }
}
