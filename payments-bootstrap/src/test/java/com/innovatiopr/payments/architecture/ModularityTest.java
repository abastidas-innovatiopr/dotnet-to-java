package com.innovatiopr.payments.architecture;

import com.innovatiopr.payments.PaymentsApplication;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the modular monolith.
 *
 * <p>Spring Modulith derives the module structure from the package layout: each direct sub-package of the
 * application package is a module, its root package is that module's published API, and everything
 * beneath is internal. {@code verify()} then checks two things that are otherwise impossible to enforce
 * by review alone — that no module reaches into another's internals, and that the dependency graph has no
 * cycles.
 */
class ModularityTest {

    private final ApplicationModules modules = ApplicationModules.of(PaymentsApplication.class);

    @Test
    void the_module_structure_is_valid() {
        // Fails if any module imports another module's internal package, or if two modules import each
        // other. This is the test that keeps `payments -> ledger` a one-way arrow: it is why the Ledger
        // declares its own PostingReference instead of importing TransactionId from Payments.
        modules.verify();
    }

    @Test
    void every_bounded_context_is_recognised_as_a_module() {
        assertThat(modules.stream().map(module -> module.getIdentifier().toString()))
                .contains("customers", "accounts", "ledger", "payments", "shared");
    }

    @Test
    void prints_the_module_structure() {
        // Readable in the build log; useful when onboarding someone to the codebase.
        modules.forEach(System.out::println);
    }

    /**
     * Generates module documentation into {@code target/spring-modulith-docs}: a C4 component diagram and
     * a PlantUML dependency graph per module, plus an application module canvas listing each module's
     * published types, events published and events consumed.
     *
     * <p>Generated from the code, so unlike a hand-drawn diagram it cannot drift out of date.
     */
    @Test
    void generates_module_documentation() throws Exception {
        new Documenter(modules)
                .writeModulesAsPlantUml()
                .writeIndividualModulesAsPlantUml()
                .writeModuleCanvases();
    }
}
