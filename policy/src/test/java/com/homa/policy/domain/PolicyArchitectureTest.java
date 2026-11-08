package com.homa.policy.domain;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** Dependency-rule gate: the domain imports only the shared kernel and the Java standard library. */
@AnalyzeClasses(packages = "com.homa.policy", importOptions = ImportOption.DoNotIncludeTests.class)
class PolicyArchitectureTest {

    @ArchTest
    static final ArchRule domainDependsOnlyOnItselfTheKernelAndJava = classes()
            .that().resideInAPackage("com.homa.policy.domain..")
            .should().onlyDependOnClassesThat()
            .resideInAnyPackage("com.homa.policy.domain..", "com.homa.kernel..", "java..");

    @ArchTest
    static final ArchRule domainExceptionsLiveInTheExceptionPackage = classes()
            .that().areAssignableTo(Throwable.class).and().resideInAPackage("com.homa.policy.domain..")
            .should().resideInAPackage("com.homa.policy.domain.exception");
}
