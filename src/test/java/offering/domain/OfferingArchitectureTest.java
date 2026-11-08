package offering.domain;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** Dependency-rule gate: the domain imports only the shared kernel and the Java standard library. */
@AnalyzeClasses(packages = "offering", importOptions = ImportOption.DoNotIncludeTests.class)
class OfferingArchitectureTest {

    @ArchTest
    static final ArchRule domainDependsOnlyOnItselfTheKernelAndJava = classes()
            .that().resideInAPackage("offering.domain..")
            .should().onlyDependOnClassesThat()
            .resideInAnyPackage("offering.domain..", "kernel..", "java..");

    @ArchTest
    static final ArchRule domainExceptionsLiveInTheExceptionPackage = classes()
            .that().areAssignableTo(Throwable.class).and().resideInAPackage("offering.domain..")
            .should().resideInAPackage("offering.domain.exception");
}
