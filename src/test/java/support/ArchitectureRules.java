package support;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;

/** The dependency rules of the backend module discipline (sections 2 and 5), checked per module. */
public final class ArchitectureRules {

    private ArchitectureRules() {
    }

    /** kernel/ depends on nothing but the Java standard library. */
    public static void kernelDependsOnNothing() {
        onlyDependsOn("kernel", "kernel..", "java..");
    }

    /** ie/ depends only on kernel/. */
    public static void integrationEventsDependOnlyOnKernel() {
        onlyDependsOn("ie", "ie..", "kernel..", "java..");
    }

    /** A domain layer depends only on itself and kernel/: no usecase, infra, ie, other context or framework. */
    public static void domainDependsOnlyOnKernel(String context) {
        onlyDependsOn(context + ".domain", context + ".domain..", "kernel..", "java..");
    }

    /** A usecase layer depends only on its own domain and the shared modules. */
    public static void usecaseDependsOnlyOnDomainAndShared(String context) {
        onlyDependsOn(context + ".usecase", context + ".usecase..", context + ".domain..", "kernel..", "ie..", "java..");
    }

    private static void onlyDependsOn(String module, String... allowed) {
        JavaClasses production = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(module);
        classes().that().resideInAPackage(module + "..")
                .should().onlyDependOnClassesThat().resideInAnyPackage(allowed)
                .check(production);
    }
}
