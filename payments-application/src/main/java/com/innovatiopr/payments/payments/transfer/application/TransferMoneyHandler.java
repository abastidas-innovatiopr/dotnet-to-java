package com.innovatiopr.payments.payments.transfer.application;

import com.innovatiopr.payments.payments.application.DuplicateIdempotencyKeyException;
import com.innovatiopr.payments.payments.domain.IdempotencyRecord;
import com.innovatiopr.payments.payments.domain.TransferError;
import com.innovatiopr.payments.shared.application.CommandHandler;
import com.innovatiopr.payments.shared.domain.Result;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Transfers money between two accounts, exactly once per idempotency key.
 *
 * <h2>The three cases</h2>
 * <ol>
 *   <li><b>New key</b> — do the work in {@link TransferMoneyOperation} and store the key with the result.</li>
 *   <li><b>Known key, same request</b> — return the original outcome without moving money again. A client
 *       that retried after a timeout gets the same answer it would have got the first time.</li>
 *   <li><b>Known key, different request</b> — refuse with a conflict. Replaying the stored result would
 *       answer a question the client did not ask, and processing the new body would break the promise the
 *       key encodes.</li>
 * </ol>
 *
 * <h2>Why this class has no transaction of its own</h2>
 * The work runs in {@link TransferMoneyOperation}'s transaction. When two requests race on the same key,
 * the loser's transaction is aborted by the unique-index violation — and an aborted PostgreSQL transaction
 * cannot be read from. The retry lookup therefore has to happen after that transaction has unwound, which
 * is only possible if this method is not inside it. {@link TransferReplayReader} then opens a genuinely
 * new transaction to read the winner.
 */
@Service
public class TransferMoneyHandler implements CommandHandler<TransferMoneyCommand, TransferMoneyResult> {

    private final TransferMoneyOperation operation;
    private final TransferReplayReader replayReader;

    TransferMoneyHandler(TransferMoneyOperation operation, TransferReplayReader replayReader) {
        this.operation = operation;
        this.replayReader = replayReader;
    }

    @Override
    public Result<TransferMoneyResult> handle(TransferMoneyCommand command) {
        Optional<IdempotencyRecord> alreadySeen = replayReader.find(command.idempotencyKey());
        if (alreadySeen.isPresent()) {
            return replayReader.replay(alreadySeen.get(), command.requestHash());
        }

        try {
            return operation.execute(command);
        } catch (DuplicateIdempotencyKeyException e) {
            // A concurrent request claimed the key while we were working. Our transaction rolled back, so
            // no money moved. The winner's record is committed and visible in a fresh transaction.
            return replayReader.find(command.idempotencyKey())
                    .map(record -> replayReader.replay(record, command.requestHash()))
                    .orElseGet(() -> Result.failure(
                            TransferError.idempotencyKeyReused(command.idempotencyKey().value())));
        }
    }
}
