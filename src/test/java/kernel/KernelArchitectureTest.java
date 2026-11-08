package kernel;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** Dependency-rule gate: kernel/ imports nothing but the Java standard library (Discipline §5, rule 1). */
@AnalyzeClasses(packages = "kernel", importOptions = ImportOption.DoNotIncludeTests.class)
class KernelArchitectureTest {

    @ArchTest
    static final ArchRule kernelDependsOnlyOnTheJavaStandardLibrary = classes()
            .should().onlyDependOnClassesThat()
            .resideInAnyPackage("kernel..", "java..");

    @ArchTest
    static final ArchRule kernelHoldsOnlyImmutableValues = classes()
            .that().arePublic()
            .should().beRecords();
}
