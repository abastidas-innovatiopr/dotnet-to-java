package com.innovatiopr.payments.architecture;

import com.innovatiopr.payments.PaymentsApplication;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.base.DescribedPredicate.describe;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Executable architecture rules.
 *
 * <p>Every principle stated in the README is asserted here. A layering rule that lives only in a document
 * is a suggestion; one that fails the build is a constraint. The .NET equivalent would be NetArchTest or
 * ArchUnitNET.
 */
class ArchitectureTest {

    private static final String ROOT = "com.innovatiopr.payments";

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(ROOT);
    }

    @Nested
    @DisplayName("the domain depends on nothing outside itself")
    class DomainPurity {

        @Test
        void domain_does_not_depend_on_spring() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAnyPackage("org.springframework..")
                    .because("the domain must be testable and deployable without Spring; Spring is an "
                            + "implementation detail that surrounds the model, not part of it");
            rule.check(classes);
        }

        @Test
        void domain_does_not_depend_on_jpa_or_hibernate() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAnyPackage("jakarta.persistence..", "org.hibernate..")
                    .because("aggregates are plain objects; persistence is mapped by a separate JPA entity");
            rule.check(classes);
        }

        @Test
        void domain_does_not_depend_on_http_or_the_servlet_api() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "jakarta.servlet..", "org.springframework.web..", "org.springframework.http..")
                    .because("HTTP is one possible transport, not a property of the business model");
            rule.check(classes);
        }

        @Test
        void domain_does_not_depend_on_hateoas() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAnyPackage("org.springframework.hateoas..")
                    .because("hypermedia is an API representation concern");
            rule.check(classes);
        }

        @Test
        void domain_does_not_depend_on_jackson() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "tools.jackson..", "com.fasterxml.jackson..")
                    .because("serialisation format is not a domain concern");
            rule.check(classes);
        }

        @Test
        void domain_does_not_depend_on_bean_validation() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAnyPackage("jakarta.validation..")
                    .because("transport validation stops malformed input; domain invariants are enforced "
                            + "by the aggregates themselves and cannot be expressed as field annotations");
            rule.check(classes);
        }

        @Test
        void domain_does_not_depend_on_infrastructure_or_api() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAnyPackage("..infrastructure..", "..api..")
                    .because("dependencies point inward: infrastructure -> application -> domain");
            rule.check(classes);
        }

        @Test
        void domain_does_not_depend_on_the_application_layer() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAPackage("..application..")
                    .because("the domain is the innermost layer");
            rule.check(classes);
        }
    }

    @Nested
    @DisplayName("the application layer stays free of transport and persistence")
    class ApplicationLayer {

        @Test
        void application_does_not_depend_on_api() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..application..")
                    .should().dependOnClassesThat().resideInAPackage("..api..")
                    .because("use cases must be invokable without HTTP - from a test, a scheduler or a "
                            + "message consumer");
            rule.check(classes);
        }

        @Test
        void application_does_not_depend_on_infrastructure() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..application..")
                    .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
                    .because("the application layer declares ports; infrastructure implements them");
            rule.check(classes);
        }

        @Test
        void application_does_not_expose_spring_data_pagination() {
            ArchRule rule = noClasses()
                    .that().resideInAnyPackage("..application..", "..domain..")
                    .should().dependOnClassesThat().haveFullyQualifiedName("org.springframework.data.domain.Page")
                    .orShould().dependOnClassesThat().haveFullyQualifiedName("org.springframework.data.domain.Pageable")
                    .orShould().dependOnClassesThat().haveFullyQualifiedName("org.springframework.data.domain.Sort")
                    .because("PageRequest/PageResult are the framework-neutral pagination model; letting "
                            + "Spring Data's types leak would couple every use case and response to it");
            rule.check(classes);
        }

        @Test
        void application_does_not_use_jpa_directly() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..application..")
                    .should().dependOnClassesThat().resideInAnyPackage("jakarta.persistence..", "org.hibernate..")
                    .because("handlers talk to repository ports, never to an EntityManager");
            rule.check(classes);
        }
    }

    @Nested
    @DisplayName("infrastructure stays where it belongs")
    class InfrastructureBoundaries {

        @Test
        void jpa_entities_live_only_in_infrastructure_packages() {
            ArchRule rule = classes()
                    .that().areAnnotatedWith(jakarta.persistence.Entity.class)
                    .should().resideInAPackage("..infrastructure..")
                    .because("a persistence entity is an infrastructure detail; the aggregate is the model");
            rule.check(classes);
        }

        @Test
        void the_generic_hibernate_repository_is_used_only_by_infrastructure() {
            ArchRule rule = classes()
                    .that().areAssignableTo(
                            com.innovatiopr.payments.shared.infrastructure.GenericHibernateRepository.class)
                    .should().resideInAPackage("..infrastructure..")
                    .because("the generic repository removes persistence boilerplate; it is not a domain "
                            + "contract and must never become one");
            rule.check(classes);
        }

        @Test
        void only_infrastructure_uses_the_entity_manager() {
            ArchRule rule = noClasses()
                    .that().resideOutsideOfPackages("..infrastructure..")
                    .should().dependOnClassesThat().haveFullyQualifiedName("jakarta.persistence.EntityManager")
                    .because("the persistence context is an infrastructure concern");
            rule.check(classes);
        }

        @Test
        void only_infrastructure_uses_jdbc_client() {
            ArchRule rule = noClasses()
                    .that().resideOutsideOfPackages("..infrastructure..")
                    .should().dependOnClassesThat()
                    .haveFullyQualifiedName("org.springframework.jdbc.core.simple.JdbcClient")
                    .because("read models are infrastructure adapters behind an application port");
            rule.check(classes);
        }
    }

    @Nested
    @DisplayName("the API layer stays thin")
    class ApiLayer {

        @Test
        void controllers_live_only_in_api_packages() {
            ArchRule rule = classes()
                    .that().areAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
                    .should().resideInAPackage("..api..")
                    .andShould().haveSimpleNameEndingWith("Controller")
                    .because("HTTP endpoints belong to the API layer, and the name says what a class is");
            rule.check(classes);
        }

        @Test
        void anything_named_a_controller_really_is_one() {
            // The inverse of the rule above. Without it a class could be named *Controller, look like an
            // endpoint to a reader, and be mapped to nothing at all because the annotation was forgotten.
            ArchRule rule = classes()
                    .that().haveSimpleNameEndingWith("Controller")
                    .should().beAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
                    .because("a class that reads as an endpoint must actually be mapped as one");
            rule.check(classes);
        }

        @Test
        void the_api_layer_does_not_validate_by_hand() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..api..")
                    .should().dependOnClassesThat().haveFullyQualifiedName("jakarta.validation.Validator")
                    .because("@Valid on a controller parameter runs Bean Validation before the method is "
                            + "entered, and MethodArgumentNotValidException reports every offending field. "
                            + "Injecting the Validator to call it by hand is the workaround functional "
                            + "routing needed and annotated controllers do not");
            rule.check(classes);
        }

        @Test
        void the_api_layer_never_touches_persistence() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..api..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "jakarta.persistence..", "org.hibernate..", "org.springframework.jdbc..")
                    .because("endpoints invoke handlers; they do not read or write the database");
            rule.check(classes);
        }

        @Test
        void transactions_are_never_declared_on_controllers() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..api..")
                    .should().beAnnotatedWith(org.springframework.transaction.annotation.Transactional.class)
                    .because("a controller is a proxied Spring bean, so @Transactional here is NOT inert - "
                            + "it opens a real transaction that then wraps JSON serialization, HATEOAS "
                            + "link assembly and the whole response write, holding a database connection "
                            + "for all of it. Transaction boundaries belong on application handlers");
            rule.check(classes);
        }
    }

    @Nested
    @DisplayName("failures are thrown, not returned")
    class ErrorModel {

        @Test
        void the_old_result_type_has_not_come_back() {
            // The Result<T>/DomainError/ErrorType trio was removed in favour of typed exceptions. It is
            // the kind of thing that grows back one helper at a time, so the absence is asserted.
            ArchRule rule = noClasses()
                    .that().resideInAPackage(ROOT + "..")
                    .should().haveSimpleName("Result")
                    .orShould().haveSimpleName("DomainError")
                    .orShould().haveSimpleName("ErrorType")
                    .because("expected business failures are exceptions here; a Result type returned from "
                            + "a @Transactional method commits the very writes it is reporting a failure "
                            + "about");
            rule.check(classes);
        }

        @Test
        void error_factories_are_final_utility_classes() {
            ArchRule rule = classes()
                    .that().haveSimpleNameEndingWith("Errors")
                    .should().haveModifier(com.tngtech.archunit.core.domain.JavaModifier.FINAL)
                    .because("a *Errors class is a namespace for static factories, not a type to extend");
            rule.check(classes);
        }

        @Test
        void error_factories_only_produce_exceptions() {
            ArchRule rule = methods()
                    .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("Errors")
                    .and().arePublic()
                    .should().haveRawReturnType(describe("an unchecked exception",
                            javaClass -> javaClass.isAssignableTo(RuntimeException.class)))
                    .because("pairing a code with its message is the whole job of these classes; one that "
                            + "returned a value would mean a failure could be built and then ignored");
            rule.check(classes);
        }

        @Test
        void domain_exceptions_are_unchecked() {
            ArchRule rule = classes()
                    .that().resideInAPackage("..domain..")
                    .and().areAssignableTo(Exception.class)
                    .should().beAssignableTo(RuntimeException.class)
                    .because("a checked exception in an aggregate's signature would poison every lambda "
                            + "that calls it - BalanceMovement, StatusTransition and the ledger's Supplier "
                            + "are all functional interfaces whose methods declare no throws clause");
            rule.check(classes);
        }
    }

    @Nested
    @DisplayName("general hygiene")
    class Hygiene {

        @Test
        void no_field_injection_anywhere_we_control() {
            ArchRule rule = noClasses()
                    .that().resideOutsideOfPackage("com.innovatiopr.payments.shared.infrastructure")
                    .should().beAnnotatedWith(org.springframework.beans.factory.annotation.Autowired.class)
                    .because("constructor injection makes dependencies explicit, allows final fields and "
                            + "lets a class be built in a unit test without a container. The one exception "
                            + "is GenericHibernateRepository's @PersistenceContext EntityManager, which "
                            + "subclasses inherit");
            rule.check(classes);
        }

        @Test
        void the_application_class_is_where_it_should_be() {
            ArchRule rule = classes()
                    .that().haveSimpleName("PaymentsApplication")
                    .should().resideInAPackage(PaymentsApplication.class.getPackageName());
            rule.check(classes);
        }
    }
}
