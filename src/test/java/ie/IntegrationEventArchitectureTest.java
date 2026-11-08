package ie;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Dependency-rule gate for the integration event contracts: {@code ie} depends only on {@code kernel} and
 * the Java standard library, holds only event interfaces and immutable event records, and no domain
 * imports it.
 */
@AnalyzeClasses(
        packages = {"ie", "kernel", "catalogue", "policy", "account", "offering", "availability", "reservation",
            "payment", "resolution", "conversation", "discovery", "feedback"},
        importOptions = ImportOption.DoNotIncludeTests.class)
class IntegrationEventArchitectureTest {

    @ArchTest
    static final ArchRule ieDependsOnlyOnTheKernelAndJava = classes()
            .that().resideInAPackage("ie..")
            .should().onlyDependOnClassesThat()
            .resideInAnyPackage("ie..", "kernel..", "java..");

    @ArchTest
    static final ArchRule eventInterfacesAreOnlyInterfaces = classes()
            .that().resideInAPackage("ie.port..")
            .should().beInterfaces();

    @ArchTest
    static final ArchRule eventsAreRecordsNamedForTheirEventThatImplementAnEventInterface = classes()
            .that().resideInAPackage("ie.events..")
            .should().beRecords()
            .andShould().haveSimpleNameEndingWith("IE")
            .andShould().implement(com.tngtech.archunit.base.DescribedPredicate.describe(
                    "an event interface in ie.port", type -> type.getPackageName().equals("ie.port")));

    @ArchTest
    static final ArchRule noDomainImportsTheEventContracts = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAPackage("ie..");
}
