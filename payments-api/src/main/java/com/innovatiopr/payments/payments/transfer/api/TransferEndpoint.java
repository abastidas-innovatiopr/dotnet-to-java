package com.innovatiopr.payments.payments.transfer.api;

import com.innovatiopr.payments.accounts.AccountId;
import com.innovatiopr.payments.payments.domain.IdempotencyKey;
import com.innovatiopr.payments.payments.transfer.application.TransferMoneyCommand;
import com.innovatiopr.payments.payments.transfer.application.TransferMoneyHandler;
import com.innovatiopr.payments.payments.transfer.application.TransferMoneyResult;
import com.innovatiopr.payments.shared.api.ApiPaths;
import com.innovatiopr.payments.shared.api.ApiResponses;
import com.innovatiopr.payments.shared.api.RequestValidator;
import com.innovatiopr.payments.shared.application.RequestHasher;
import com.innovatiopr.payments.shared.domain.Result;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.net.URI;
import java.util.Locale;
import java.util.Optional;

/**
 * The transfer endpoint — the application's primary workflow.
 *
 * <p>Still a thin HTTP layer: it reads the header and body, validates transport constraints, computes the
 * request fingerprint, builds a command and maps the result. Locking, balance rules, ledger postings and
 * the idempotency guarantee all live behind {@code TransferMoneyHandler}.
 */
@Component
class TransferEndpoint {

    static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    private final TransferMoneyHandler transferMoney;
    private final TransferResourceAssembler assembler;
    private final RequestValidator validator;

    TransferEndpoint(TransferMoneyHandler transferMoney, TransferResourceAssembler assembler,
                     RequestValidator validator) {
        this.transferMoney = transferMoney;
        this.assembler = assembler;
        this.validator = validator;
    }

    ServerResponse transfer(ServerRequest request) throws Exception {
        String rawKey = request.headers().firstHeader(IDEMPOTENCY_KEY_HEADER);
        Result<IdempotencyKey> idempotencyKey = IdempotencyKey.create(rawKey);
        if (idempotencyKey.isFailure()) {
            return ApiResponses.problem(idempotencyKey.errors());
        }

        TransferRequest body = request.body(TransferRequest.class);
        Optional<ServerResponse> invalid = validator.validate(body);
        if (invalid.isPresent()) {
            return invalid.get();
        }

        TransferMoneyCommand command = new TransferMoneyCommand(
                idempotencyKey.orElseThrow(),
                RequestHasher.sha256(body.canonicalForm()),
                AccountId.of(body.sourceAccountId()),
                AccountId.of(body.destinationAccountId()),
                body.amount(),
                body.currency().toUpperCase(Locale.ROOT),
                body.reference());

        Result<TransferMoneyResult> result = transferMoney.handle(command);

        String baseUrl = ApiPaths.baseUrl(request);
        return ApiResponses.createdOrReplayed(result,
                transfer -> assembler.toResource(transfer, baseUrl),
                transfer -> URI.create(ApiPaths.transaction(baseUrl, transfer.transactionId())),
                TransferMoneyResult::replayed);
    }
}
