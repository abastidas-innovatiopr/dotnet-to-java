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

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
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
        void router_functions_live_only_in_api_packages() {
            ArchRule rule = classes()
                    .that().haveSimpleNameEndingWith("Routes")
                    .should().resideInAPackage("..api..")
                    .because("functional endpoint declarations belong to the API layer");
            rule.check(classes);
        }

        @Test
        void endpoints_live_only_in_api_packages() {
            ArchRule rule = classes()
                    .that().haveSimpleNameEndingWith("Endpoint")
                    .should().resideInAPackage("..api..")
                    .because("handler functions belong to the API layer");
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
        void transactions_are_never_declared_on_endpoints_or_routes() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..api..")
                    .should().beAnnotatedWith(org.springframework.transaction.annotation.Transactional.class)
                    .because("@Transactional on a RouterFunction bean or a handler function is silently "
                            + "inert - the handler is invoked as a method reference, not through a Spring "
                            + "proxy. Transaction boundaries belong on application handlers");
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
