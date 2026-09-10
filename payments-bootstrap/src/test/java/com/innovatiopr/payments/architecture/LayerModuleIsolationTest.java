package com.innovatiopr.payments.architecture;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Asserts the property that makes this a multi-module build rather than a package convention:
 * <b>the framework is not on the domain module's compile classpath at all.</b>
 *
 * <h2>Why this is not just another ArchUnit rule</h2>
 * {@code ArchitectureTest} checks that no domain class <em>imports</em> Spring. This checks something
 * stronger and earlier: that it <em>could not</em>. A rule can be deleted, suppressed, or quietly
 * outvoted in review; a dependency that is absent from the POM makes the import fail to resolve, and no
 * amount of pressure on a deadline gets around a compiler error.
 *
 * <p>The two are complementary. ArchUnit covers what a POM cannot express — no JPA entity outside
 * infrastructure, no {@code @Transactional} on an endpoint, no Spring Data {@code Page} in a use case.
 * This test covers what only the build can guarantee.
 *
 * <p>It reads the POMs as text on purpose. Parsing the effective POM would test Maven; reading the
 * declared dependencies tests the decision a developer actually makes when they add one.
 */
class LayerModuleIsolationTest {

    /** Repository root, from the bootstrap module's working directory. */
    private static final Path ROOT = Path.of("..");

    private static final List<String> FRAMEWORK_ARTIFACTS = List.of(
            "spring-boot-starter-webmvc",
            "spring-boot-starter-data-jpa",
            "spring-boot-starter-jdbc",
            "spring-boot-starter-hateoas",
            "spring-boot-starter-validation",
            "spring-boot-starter-flyway",
            "hibernate",
            "postgresql",
            "jackson",
            "springdoc");

    private static String pomOf(String module) throws IOException {
        return Files.readString(ROOT.resolve(module).resolve("pom.xml"));
    }

    @Test
    @DisplayName("the domain module declares no framework dependency of any kind")
    void domain_has_no_framework_on_its_classpath() throws IOException {
        String pom = pomOf("payments-domain");

        assertThat(FRAMEWORK_ARTIFACTS)
                .allSatisfy(artifact -> assertThat(pom)
                        .as("payments-domain must not depend on " + artifact)
                        .doesNotContain(artifact));

        // The one permitted dependency is annotations-only and provided-scope.
        assertThat(pom).contains("spring-modulith-api");
        assertThat(pom).contains("<scope>provided</scope>");
    }

    @Test
    @DisplayName("the domain module depends on no other module of this project")
    void domain_is_a_leaf() throws IOException {
        String pom = pomOf("payments-domain");

        assertThat(pom)
                .doesNotContain("payments-application")
                .doesNotContain("payments-infrastructure")
                .doesNotContain("payments-api")
                .doesNotContain("payments-bootstrap");
    }

    @Test
    @DisplayName("the application module sees the domain but no persistence or web stack")
    void application_declares_only_the_domain_and_a_container() throws IOException {
        String pom = pomOf("payments-application");

        assertThat(pom).contains("payments-domain");

        // A handler cannot reach for an EntityManager or a JdbcClient: neither is present.
        assertThat(pom)
                .doesNotContain("spring-boot-starter-data-jpa")
                .doesNotContain("spring-boot-starter-jdbc")
                .doesNotContain("spring-boot-starter-webmvc")
                .doesNotContain("spring-boot-starter-hateoas")
                .doesNotContain("hibernate")
                .doesNotContain("postgresql");

        assertThat(pom)
                .doesNotContain("payments-infrastructure")
                .doesNotContain("payments-api");
    }

    @Test
    @DisplayName("api and infrastructure cannot see each other")
    void the_two_adapter_modules_are_mutually_invisible() throws IOException {
        // An endpoint importing a JPA entity, or a repository importing a resource assembler, is a
        // build failure rather than a review comment - neither module is on the other's classpath.
        assertThat(pomOf("payments-api"))
                .as("payments-api must not depend on payments-infrastructure")
                .doesNotContain("payments-infrastructure");

        assertThat(pomOf("payments-infrastructure"))
                .as("payments-infrastructure must not depend on payments-api")
                .doesNotContain("payments-api</artifactId>");
    }

    @Test
    @DisplayName("the api module has no persistence stack")
    void api_cannot_reach_the_database() throws IOException {
        String pom = pomOf("payments-api");

        assertThat(pom).contains("payments-application");
        assertThat(pom)
                .doesNotContain("spring-boot-starter-data-jpa")
                .doesNotContain("spring-boot-starter-jdbc")
                .doesNotContain("hibernate")
                .doesNotContain("postgresql");
    }

    @Test
    @DisplayName("only the bootstrap module is packaged as a runnable application")
    void one_composition_root() throws IOException {
        assertThat(pomOf("payments-bootstrap")).contains("spring-boot-maven-plugin");

        for (String library : List.of("payments-domain", "payments-application",
                "payments-infrastructure", "payments-api")) {
            assertThat(pomOf(library))
                    .as(library + " is a library and must not be repackaged into an executable jar")
                    .doesNotContain("spring-boot-maven-plugin");
        }
    }

    @Test
    @DisplayName("every layer module is registered in the parent build")
    void the_parent_aggregates_all_modules() throws IOException {
        String parent = Files.readString(ROOT.resolve("pom.xml"));

        try (Stream<String> ignored = Stream.empty()) {
            assertThat(parent)
                    .contains("<module>payments-domain</module>")
                    .contains("<module>payments-application</module>")
                    .contains("<module>payments-infrastructure</module>")
                    .contains("<module>payments-api</module>")
                    .contains("<module>payments-bootstrap</module>");
        }
    }
}
