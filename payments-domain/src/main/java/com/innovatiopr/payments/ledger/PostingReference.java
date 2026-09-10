package com.innovatiopr.payments.ledger;

import java.util.Objects;
import java.util.UUID;

/**
 * The Ledger's own identifier for "the business operation that caused these postings".
 *
 * <h2>Why Ledger does not import {@code TransactionId}</h2>
 * Payments owns {@code TransactionId} and Payments calls the Ledger. If the Ledger imported
 * {@code TransactionId}, the two modules would import each other and Spring Modulith would fail the build
 * on a module cycle — correctly, because a cycle means neither module can be understood or deployed alone.
 *
 * <p>So the Ledger declares the concept it needs on its own terms and Payments translates at the boundary
 * ({@code new PostingReference(transactionId.value())}). This is a miniature anti-corruption layer, and it
 * is what keeps the dependency arrow pointing one way: {@code payments → ledger}.
 */
public record PostingReference(UUID value) {

    public PostingReference {
        Objects.requireNonNull(value, "value");
    }

    public static PostingReference of(UUID value) {
        return new PostingReference(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
