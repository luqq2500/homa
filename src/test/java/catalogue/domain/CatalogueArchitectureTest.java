package catalogue.domain;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Dependency-rule gate: the domain imports only the shared kernel and the Java standard library; the
 * usecase layer only its own domain, the kernel, the integration events and Java.
 */
@AnalyzeClasses(packages = "catalogue", importOptions = ImportOption.DoNotIncludeTests.class)
class CatalogueArchitectureTest {

    @ArchTest
    static final ArchRule domainDependsOnlyOnItselfTheKernelAndJava = classes()
            .that().resideInAPackage("catalogue.domain..")
            .should().onlyDependOnClassesThat()
            .resideInAnyPackage("catalogue.domain..", "kernel..", "java..");

    @ArchTest
    static final ArchRule usecaseDependsOnlyOnItsOwnDomainTheKernelEventsAndJava = classes()
            .that().resideInAPackage("catalogue.usecase..")
            .should().onlyDependOnClassesThat()
            .resideInAnyPackage("catalogue.domain..", "catalogue.usecase..", "kernel..", "ie..", "java..");

    @ArchTest
    static final ArchRule portsAreInterfaces = classes()
            .that().resideInAnyPackage("catalogue.usecase.port.in..", "catalogue.usecase.port.out..")
            .should().beInterfaces();

    @ArchTest
    static final ArchRule dtosAreRecords = classes()
            .that().resideInAPackage("catalogue.usecase.dto..").and().areTopLevelClasses()
            .should().beRecords();

    @ArchTest
    static final ArchRule everyUsecaseImplementsAnInPort = classes()
            .that().resideInAPackage("catalogue.usecase").and().areTopLevelClasses()
            .should().implement(DescribedPredicate.describe(
                    "an interface in catalogue.usecase.port.in", type -> type.getPackageName().equals("catalogue.usecase.port.in")));

    @ArchTest
    static final ArchRule usecaseExceptionsLiveInTheExceptionPackage = classes()
            .that().areAssignableTo(Throwable.class).and().resideInAPackage("catalogue.usecase..")
            .should().resideInAPackage("catalogue.usecase.exception");

    @ArchTest
    static final ArchRule domainExceptionsLiveInTheExceptionPackage = classes()
            .that().areAssignableTo(Throwable.class).and().resideInAPackage("catalogue.domain..")
            .should().resideInAPackage("catalogue.domain.exception");
}
