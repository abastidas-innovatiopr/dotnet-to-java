/**
 * Shared kernel: the types every bounded context is allowed to depend on.
 *
 * <p>Declared {@code OPEN} so that its sub-packages ({@code domain}, {@code application},
 * {@code infrastructure}, {@code api}) are visible to other modules. A closed module exposes only its
 * root package, which would force {@code Money} and {@code Result} into one flat package and destroy the
 * Clean Architecture layering that ArchUnit enforces here.
 *
 * <p>A shared kernel is a liability as well as a convenience: everything in it is coupled to everything
 * that uses it. It is kept deliberately small — value objects, the {@code Result} type, the error and
 * event contracts, and cross-cutting API plumbing. Business rules never belong here.
 */
@org.springframework.modulith.ApplicationModule(
        type = org.springframework.modulith.ApplicationModule.Type.OPEN,
        displayName = "Shared Kernel")
package com.innovatiopr.payments.shared;
