package account.usecase;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Dependency-rule gate for the account usecase layer: it depends only on its own domain, the kernel
 * and the event contracts; ports are interfaces; DTOs are records that carry no domain object; and each
 * usecase implements exactly one in port, which no other usecase implements.
 */
@AnalyzeClasses(packages = "account", importOptions = ImportOption.DoNotIncludeTests.class)
class AccountUsecaseArchitectureTest {

    private static final DescribedPredicate<JavaClass> AN_IN_PORT = DescribedPredicate.describe(
            "a usecase interface in account.usecase.port.in",
            type -> type.getPackageName().equals("account.usecase.port.in"));

    @ArchTest
    static final ArchRule usecaseDependsOnlyOnItsDomainTheKernelTheEventContractsAndJava = classes()
            .that().resideInAPackage("account.usecase..")
            .should().onlyDependOnClassesThat()
            .resideInAnyPackage("account.domain..", "account.usecase..", "kernel..", "ie..", "java..");

    @ArchTest
    static final ArchRule theDomainDoesNotKnowTheUsecaseLayer = noClasses()
            .that().resideInAPackage("account.domain..")
            .should().dependOnClassesThat().resideInAPackage("account.usecase..");

    @ArchTest
    static final ArchRule inPortsAreInterfaces = classes()
            .that().resideInAPackage("account.usecase.port.in")
            .should().beInterfaces();

    @ArchTest
    static final ArchRule outPortsAreInterfacesOrTheRecordsTheyCarry = classes()
            .that().resideInAPackage("account.usecase.port.out")
            .should().beInterfaces().orShould().beRecords();

    @ArchTest
    static final ArchRule dtosAreRecords = classes()
            .that().resideInAPackage("account.usecase.dto")
            .should().beRecords();

    @ArchTest
    static final ArchRule domainObjectsNeverLeaveThroughInPortsOrDtos = noClasses()
            .that().resideInAnyPackage("account.usecase.port.in", "account.usecase.dto")
            .should().dependOnClassesThat().resideInAnyPackage("account.domain..", "ie..");

    @ArchTest
    static final ArchRule everyUsecaseImplementsExactlyOneInPort = classes()
            .that().resideInAPackage("account.usecase").and().arePublic()
            .should(new ArchCondition<JavaClass>("implement exactly one in port") {
                @Override
                public void check(JavaClass usecase, ConditionEvents events) {
                    long ports = usecase.getRawInterfaces().stream().filter(AN_IN_PORT).count();
                    if (ports != 1) {
                        events.add(SimpleConditionEvent.violated(usecase, usecase.getName() + " implements " + ports + " in ports"));
                    }
                }
            });

    @ArchTest
    static final ArchRule everyInPortHasExactlyOneUsecase = classes()
            .that().resideInAPackage("account.usecase.port.in")
            .should(new ArchCondition<JavaClass>("be implemented by exactly one usecase") {
                @Override
                public void check(JavaClass port, ConditionEvents events) {
                    long usecases = port.getAllSubclasses().stream()
                            .filter(type -> type.getPackageName().equals("account.usecase"))
                            .count();
                    if (usecases != 1) {
                        events.add(SimpleConditionEvent.violated(port, port.getName() + " has " + usecases + " usecases"));
                    }
                }
            });

    @ArchTest
    static final ArchRule usecaseExceptionsLiveInTheExceptionPackage = classes()
            .that().areAssignableTo(Throwable.class).and().resideInAPackage("account.usecase..")
            .should().resideInAPackage("account.usecase.exception");
}
