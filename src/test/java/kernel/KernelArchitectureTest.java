package kernel;

import org.junit.jupiter.api.Test;
import support.ArchitectureRules;

class KernelArchitectureTest {

    @Test
    void kernelDependsOnNothingButTheJavaStandardLibrary() {
        ArchitectureRules.kernelDependsOnNothing();
    }
}
