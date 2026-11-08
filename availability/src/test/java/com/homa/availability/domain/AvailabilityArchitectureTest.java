package com.homa.availability.domain;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** Dependency-rule gate: the domain imports only the shared kernel and the Java standard library. */
@AnalyzeClasses(packages = "com.homa.availability", importOptions = ImportOption.DoNotIncludeTests.class)
class AvailabilityArchitectureTest {

    @ArchTest
    static final ArchRule domainDependsOnlyOnItselfTheKernelAndJava = classes()
            .that().resideInAPackage("com.homa.availability.domain..")
            .should().onlyDependOnClassesThat()
            .resideInAnyPackage("com.homa.availability.domain..", "com.homa.kernel..", "java..");

    @ArchTest
    static final ArchRule domainExceptionsLiveInTheExceptionPackage = classes()
            .that().areAssignableTo(Throwable.class).and().resideInAPackage("com.homa.availability.domain..")
            .should().resideInAPackage("com.homa.availability.domain.exception");
}
