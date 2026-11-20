package com.homa.architecture;

import static com.tngtech.archunit.base.DescribedPredicate.alwaysTrue;
import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * The rules that replace module boundaries (plan, "Source layout"). Written in slice 0, before any domain code,
 * and a failing rule fails the build.
 */
@AnalyzeClasses(packages = "com.homa", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    /** Rule 1: kernel is JDK-only. */
    @ArchTest
    static final ArchRule kernelIsJdkOnly = classes()
            .that().resideInAPackage("com.homa.kernel..")
            .should().onlyDependOnClassesThat().resideInAnyPackage("com.homa.kernel..", "java..");

    /** Rule 2: events carry primitives and kernel types only. */
    @ArchTest
    static final ArchRule ieIsContractOnly = classes()
            .that().resideInAPackage("com.homa.ie..")
            .should().onlyDependOnClassesThat().resideInAnyPackage("com.homa.ie..", "com.homa.kernel..", "java..");

    /** Rule 3: the domain imports nothing outside the domain layer, and no framework. */
    @ArchTest
    static final ArchRule domainIsPure = noClasses()
            .that().resideInAPackage("com.homa.*.domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..usecase..", "..infra..", "com.homa.ie..", "com.homa.app..",
                    "org.springframework..", "jakarta..", "com.fasterxml..");

    /** Rule 3, closed list: a domain depends only on domain code, kernel and the JDK (rule 5 keeps it to its own context). */
    @ArchTest
    static final ArchRule domainDependsOnlyOnDomainKernelAndJdk = classes()
            .that().resideInAPackage("com.homa.*.domain..")
            .should().onlyDependOnClassesThat().resideInAnyPackage("com.homa.*.domain..", "com.homa.kernel..", "java..");

    /** Rule 4: a usecase reaches the outside only through port/out. */
    @ArchTest
    static final ArchRule usecaseHasNoInfra = noClasses()
            .that().resideInAPackage("com.homa.*.usecase..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..infra..", "com.homa.app..", "org.springframework..", "jakarta..", "software.amazon..");

    /** Rule 5: contexts talk only through ie events; only kernel, ie and app are shared, and only app sees every context. */
    @ArchTest
    static final ArchRule contextsAreIsolated = slices()
            .matching("com.homa.(*)..").namingSlices("$1")
            .should().notDependOnEachOther()
            .ignoreDependency(resideInAPackage("com.homa.app.."), alwaysTrue())
            .ignoreDependency(alwaysTrue(), resideInAnyPackage("com.homa.kernel..", "com.homa.ie.."));

    /** Rule 6: domain objects never cross transport; controllers use port/in and DTOs only. */
    @ArchTest
    static final ArchRule controllersUsePortsOnly = noClasses()
            .that().resideInAPackage("com.homa.*.infra.transport.controller..")
            .should().dependOnClassesThat().resideInAPackage("com.homa.*.domain..")
            .orShould().dependOnClassesThat(resideInAPackage("com.homa.*.usecase..")
                    .and(not(resideInAnyPackage("com.homa.*.usecase.port.in..", "com.homa.*.usecase.dto.."))));

    /** Rule 7: IE handler implementations live in usecase.ie ... */
    @ArchTest
    static final ArchRule ieHandlersLiveInUsecaseIe = classes()
            .that().implement(resideInAPackage("com.homa.*.usecase.port.in..")
                    .and(com.tngtech.archunit.core.domain.properties.HasName.Predicates.nameMatching(".*IEHandler")))
            .should().resideInAPackage("com.homa.*.usecase.ie..");

    /** Rule 7: ... their interfaces sit in port/in ... */
    @ArchTest
    static final ArchRule ieHandlerInterfacesLiveInPortIn = classes()
            .that().haveSimpleNameEndingWith("IEHandler")
            .should().beInterfaces()
            .andShould().resideInAPackage("com.homa.*.usecase.port.in..");

    /** Rule 7: ... and only infra.event (or the composition root) calls them. */
    @ArchTest
    static final ArchRule onlyInfraEventCallsIeHandlers = classes()
            .that().haveSimpleNameEndingWith("IEHandler")
            .should().onlyBeAccessed().byClassesThat().resideInAnyPackage(
                    "com.homa.*.infra.event..", "com.homa.*.usecase.port.in..", "com.homa.*.usecase.ie..", "com.homa.app..");
}
