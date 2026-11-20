package com.homa.architecture;

import static com.tngtech.archunit.base.DescribedPredicate.alwaysTrue;
import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

/**
 * The rules that replace module boundaries (plan, "Source layout"). Written in slice 0, before any domain code,
 * and a failing rule fails the build. Each rule is checked from a plain JUnit test so that surefire runs and
 * counts it.
 */
class ArchitectureTest {

    private static final JavaClasses PRODUCTION_CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.homa");

    /** Rule 1: kernel is JDK-only. */
    private static final ArchRule kernelIsJdkOnly = classes()
            .that().resideInAPackage("com.homa.kernel..")
            .should().onlyDependOnClassesThat().resideInAnyPackage("com.homa.kernel..", "java..");

    /** Rule 2: events carry primitives and kernel types only. */
    private static final ArchRule ieIsContractOnly = classes()
            .that().resideInAPackage("com.homa.ie..")
            .should().onlyDependOnClassesThat().resideInAnyPackage("com.homa.ie..", "com.homa.kernel..", "java..");

    /** Rule 2: an event is an immutable record named *IE that implements its publisher's interface. */
    private static final ArchRule eventsAreRecordsImplementingAPort = classes()
            .that().resideInAPackage("com.homa.ie.events..").and().areNotAnonymousClasses()
            .and().doNotHaveSimpleName("package-info")
            .should().beRecords()
            .andShould().haveSimpleNameEndingWith("IE")
            .andShould().implement(resideInAPackage("com.homa.ie.port.."));

    /** Rule 2: the ports are interfaces named after their publishing context. */
    private static final ArchRule portsAreInterfaces = classes()
            .that().resideInAPackage("com.homa.ie.port..").and().doNotHaveSimpleName("package-info")
            .should().beInterfaces()
            .andShould().haveSimpleNameEndingWith("IE");

    /** Rule 3 (allowlist): a domain sees only domain code, kernel and the JDK; rule 5 pins it to its own context. */
    private static final ArchRule domainIsPure = classes()
            .that().resideInAPackage("com.homa.*.domain..")
            .should().onlyDependOnClassesThat().resideInAnyPackage("com.homa.*.domain..", "com.homa.kernel..", "java..");

    /**
     * Rule 4 (allowlist): a usecase sees only inner layers, ie, kernel and the JDK, so it reaches the outside only
     * through port/out; rule 5 pins it to its own context.
     */
    private static final ArchRule usecaseUsesInnerLayersOnly = classes()
            .that().resideInAPackage("com.homa.*.usecase..")
            .should().onlyDependOnClassesThat().resideInAnyPackage(
                    "com.homa.*.usecase..", "com.homa.*.domain..", "com.homa.ie..", "com.homa.kernel..", "java..");

    /** Rule 5: contexts talk only through ie events; only kernel, ie and app are shared, and only app sees every context. */
    private static final ArchRule contextsAreIsolated = slices()
            .matching("com.homa.(*)..").namingSlices("$1")
            .should().notDependOnEachOther()
            .ignoreDependency(resideInAPackage("com.homa.app.."), alwaysTrue())
            .ignoreDependency(alwaysTrue(), resideInAnyPackage("com.homa.kernel..", "com.homa.ie.."));

    /** Rule 6: domain objects never cross transport; controllers use port/in and DTOs only. */
    private static final ArchRule controllersUsePortsOnly = noClasses()
            .that().resideInAPackage("com.homa.*.infra.transport.controller..")
            .should().dependOnClassesThat().resideInAPackage("com.homa.*.domain..")
            .orShould().dependOnClassesThat(resideInAPackage("com.homa.*.usecase..")
                    .and(not(resideInAnyPackage("com.homa.*.usecase.port.in..", "com.homa.*.usecase.dto.."))));

    /** Rule 7: IE handler implementations live in usecase.ie ... */
    private static final ArchRule ieHandlersLiveInUsecaseIe = classes()
            .that().implement(resideInAPackage("com.homa.*.usecase.port.in..")
                    .and(com.tngtech.archunit.core.domain.properties.HasName.Predicates.nameMatching(".*IEHandler")))
            .should().resideInAPackage("com.homa.*.usecase.ie..");

    /** Rule 7: ... their interfaces sit in port/in ... */
    private static final ArchRule ieHandlerInterfacesLiveInPortIn = classes()
            .that().haveSimpleNameEndingWith("IEHandler")
            .should().beInterfaces()
            .andShould().resideInAPackage("com.homa.*.usecase.port.in..");

    /** Rule 7: ... and only infra.event (or the composition root) calls them. */
    private static final ArchRule onlyInfraEventCallsIeHandlers = classes()
            .that().haveSimpleNameEndingWith("IEHandler")
            .should().onlyBeAccessed().byClassesThat().resideInAnyPackage(
                    "com.homa.*.infra.event..", "com.homa.*.usecase.port.in..", "com.homa.*.usecase.ie..", "com.homa.app..");

    @Test
    void kernelIsJdkOnly() {
        kernelIsJdkOnly.check(PRODUCTION_CLASSES);
    }

    @Test
    void ieIsContractOnly() {
        ieIsContractOnly.check(PRODUCTION_CLASSES);
    }

    @Test
    void eventsAreRecordsImplementingAPort() {
        eventsAreRecordsImplementingAPort.check(PRODUCTION_CLASSES);
    }

    @Test
    void portsAreInterfaces() {
        portsAreInterfaces.check(PRODUCTION_CLASSES);
    }

    @Test
    void domainIsPure() {
        domainIsPure.check(PRODUCTION_CLASSES);
    }

    @Test
    void usecaseUsesInnerLayersOnly() {
        usecaseUsesInnerLayersOnly.check(PRODUCTION_CLASSES);
    }

    @Test
    void contextsAreIsolated() {
        contextsAreIsolated.check(PRODUCTION_CLASSES);
    }

    @Test
    void controllersUsePortsOnly() {
        controllersUsePortsOnly.check(PRODUCTION_CLASSES);
    }

    @Test
    void ieHandlersLiveInUsecaseIe() {
        ieHandlersLiveInUsecaseIe.check(PRODUCTION_CLASSES);
    }

    @Test
    void ieHandlerInterfacesLiveInPortIn() {
        ieHandlerInterfacesLiveInPortIn.check(PRODUCTION_CLASSES);
    }

    @Test
    void onlyInfraEventCallsIeHandlers() {
        onlyInfraEventCallsIeHandlers.check(PRODUCTION_CLASSES);
    }
}
