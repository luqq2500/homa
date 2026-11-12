package catalogue.usecase;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Dependency-rule gate for the catalogue's usecase layer: it depends only on its own domain, the kernel
 * and the event contracts; ports are interfaces; DTOs are records that carry no domain object; and every
 * usecase implements its own port.
 */
@AnalyzeClasses(packages = "catalogue", importOptions = ImportOption.DoNotIncludeTests.class)
class CatalogueUsecaseArchitectureTest {

    private static final DescribedPredicate<JavaClass> AN_IN_PORT = DescribedPredicate.describe(
            "a usecase interface in catalogue.usecase.port.in",
            type -> type.getPackageName().equals("catalogue.usecase.port.in"));

    @ArchTest
    static final ArchRule usecaseDependsOnlyOnItsDomainTheKernelTheEventContractsAndJava = classes()
            .that().resideInAPackage("catalogue.usecase..")
            .should().onlyDependOnClassesThat()
            .resideInAnyPackage("catalogue.domain..", "catalogue.usecase..", "kernel..", "ie..", "java..");

    @ArchTest
    static final ArchRule theDomainDoesNotKnowTheUsecaseLayer = noClasses()
            .that().resideInAPackage("catalogue.domain..")
            .should().dependOnClassesThat().resideInAPackage("catalogue.usecase..");

    @ArchTest
    static final ArchRule inPortsAreInterfaces = classes()
            .that().resideInAPackage("catalogue.usecase.port.in")
            .should().beInterfaces();

    @ArchTest
    static final ArchRule outPortsAreInterfacesOrTheRecordsTheyCarry = classes()
            .that().resideInAPackage("catalogue.usecase.port.out")
            .should().beInterfaces().orShould().beRecords();

    @ArchTest
    static final ArchRule dtosAreRecords = classes()
            .that().resideInAPackage("catalogue.usecase.dto")
            .should().beRecords();

    @ArchTest
    static final ArchRule domainObjectsNeverLeaveThroughInPortsOrDtos = noClasses()
            .that().resideInAnyPackage("catalogue.usecase.port.in", "catalogue.usecase.dto")
            .should().dependOnClassesThat().resideInAnyPackage("catalogue.domain..", "ie..");

    @ArchTest
    static final ArchRule everyUsecaseImplementsItsOwnInPort = classes()
            .that().resideInAPackage("catalogue.usecase").and().arePublic()
            .should().implement(AN_IN_PORT);

    @ArchTest
    static final ArchRule usecaseExceptionsLiveInTheExceptionPackage = classes()
            .that().areAssignableTo(Throwable.class).and().resideInAPackage("catalogue.usecase..")
            .should().resideInAPackage("catalogue.usecase.exception");
}
