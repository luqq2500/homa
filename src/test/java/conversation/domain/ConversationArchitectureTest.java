package conversation.domain;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** Dependency-rule gate: the domain imports only the shared kernel and the Java standard library. */
@AnalyzeClasses(packages = "conversation", importOptions = ImportOption.DoNotIncludeTests.class)
class ConversationArchitectureTest {

    @ArchTest
    static final ArchRule domainDependsOnlyOnItselfTheKernelAndJava = classes()
            .that().resideInAPackage("conversation.domain..")
            .should().onlyDependOnClassesThat()
            .resideInAnyPackage("conversation.domain..", "kernel..", "java..");

    @ArchTest
    static final ArchRule domainExceptionsLiveInTheExceptionPackage = classes()
            .that().areAssignableTo(Throwable.class).and().resideInAPackage("conversation.domain..")
            .should().resideInAPackage("conversation.domain.exception");
}
