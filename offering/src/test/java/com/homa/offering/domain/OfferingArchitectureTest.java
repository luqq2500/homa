package com.homa.offering.domain;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** Dependency-rule gate: the domain imports only the shared kernel and the Java standard library. */
@AnalyzeClasses(packages = "com.homa.offering", importOptions = ImportOption.DoNotIncludeTests.class)
class OfferingArchitectureTest {

    @ArchTest
    static final ArchRule domainDependsOnlyOnItselfTheKernelAndJava = classes()
            .that().resideInAPackage("com.homa.offering.domain..")
            .should().onlyDependOnClassesThat()
            .resideInAnyPackage("com.homa.offering.domain..", "com.homa.kernel..", "java..");

    @ArchTest
    static final ArchRule domainExceptionsLiveInTheExceptionPackage = classes()
            .that().areAssignableTo(Throwable.class).and().resideInAPackage("com.homa.offering.domain..")
            .should().resideInAPackage("com.homa.offering.domain.exception");
}
