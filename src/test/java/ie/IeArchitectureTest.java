package ie;

import org.junit.jupiter.api.Test;
import support.ArchitectureRules;

class IeArchitectureTest {

    @Test
    void integrationEventsDependOnlyOnTheKernel() {
        ArchitectureRules.integrationEventsDependOnlyOnKernel();
    }

    @Test
    void everyEventIsARecordThatImplementsAnEventInterface() {
        ArchitectureRules.integrationEventsAreRecordsBehindAnInterface();
    }
}
