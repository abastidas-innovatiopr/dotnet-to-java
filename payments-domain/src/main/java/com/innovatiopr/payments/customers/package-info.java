/**
 * Customers bounded context: who the bank's clients are.
 *
 * <p>Depends on nothing but the shared kernel — the leaf of the module graph. It has no idea that
 * accounts or payments exist, which is what lets it be understood, tested and changed on its own.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Customers")
package com.innovatiopr.payments.customers;
