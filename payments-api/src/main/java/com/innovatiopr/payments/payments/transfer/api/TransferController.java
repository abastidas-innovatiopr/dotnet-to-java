package com.innovatiopr.payments.payments.transfer.api;

import com.innovatiopr.payments.accounts.AccountId;
import com.innovatiopr.payments.payments.domain.IdempotencyKey;
import com.innovatiopr.payments.payments.transfer.application.TransferMoneyCommand;
import com.innovatiopr.payments.payments.transfer.application.TransferMoneyHandler;
import com.innovatiopr.payments.payments.transfer.application.TransferMoneyResult;
import com.innovatiopr.payments.shared.api.ApiPaths;
import com.innovatiopr.payments.shared.api.ApiResponses;
import com.innovatiopr.payments.shared.api.docs.OpenApiDocs;
import com.innovatiopr.payments.shared.application.RequestHasher;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Locale;

/**
 * The transfer endpoint — the application's primary workflow.
 *
 * <p>Still a thin HTTP layer: it reads the header and body, computes the request fingerprint, builds a
 * command and chooses a status. Locking, balance rules, ledger postings and the idempotency guarantee all
 * live behind {@code TransferMoneyHandler}.
 */
@RestController
@RequestMapping(path = ApiPaths.TRANSFERS, produces = MediaTypes.HAL_JSON_VALUE)
@Tag(name = OpenApiDocs.TAG_PAYMENTS)
public class TransferController {

    private final TransferMoneyHandler transferMoney;
    private final TransferResourceAssembler assembler;

    TransferController(TransferMoneyHandler transferMoney, TransferResourceAssembler assembler) {
        this.transferMoney = transferMoney;
        this.assembler = assembler;
    }

    @PostMapping
    @Operation(
            operationId = "transferMoney",
            summary = "Transfer money between two accounts",
            description = """
                    The application's primary workflow. Source balance, destination balance, the payment \
                    transaction, the balanced ledger postings and the idempotency record all commit in \
                    **one database transaction**.

                    Both accounts are locked with `SELECT ... FOR UPDATE` in a deterministic ascending \
                    id order, which prevents both double spending and deadlocks between opposing \
                    transfers.

                    Requires an `Idempotency-Key`. A retried request never moves money twice, even when \
                    the retries arrive concurrently.""")
    @ApiResponse(responseCode = "201", description = "Transfer completed. Carries both resulting balances "
            + "and a `Location` header pointing at the transaction.")
    @ApiResponse(responseCode = "200", description = "Replayed from a stored idempotency record. Same body "
            + "as the original response with `replayed: true`; no money moved.")
    @ApiResponse(responseCode = "400",
            description = "Missing or malformed `Idempotency-Key`, or an invalid body.")
    @ApiResponse(responseCode = "404", description = "One of the accounts does not exist.")
    @ApiResponse(responseCode = "409",
            description = "`TRANSFER_IDEMPOTENCY_KEY_REUSED` - the key was already used for a different request.")
    @ApiResponse(responseCode = "422",
            description = "A domain rule refused the transfer: `ACCOUNT_INSUFFICIENT_FUNDS`, "
                    + "`ACCOUNT_FROZEN`, `ACCOUNT_CLOSED`, `TRANSFER_SAME_ACCOUNT` or "
                    + "`MONEY_CURRENCY_MISMATCH`.")
    public ResponseEntity<TransferResource> transfer(
            // required = false on the binding, required = true in the document: letting Spring raise
            // MissingRequestHeaderException would lose the stable TRANSFER_IDEMPOTENCY_KEY_REQUIRED code
            // that IdempotencyKey.create(null) produces, and that code is part of the published contract.
            @RequestHeader(name = OpenApiDocs.IDEMPOTENCY_KEY_HEADER, required = false)
            @Parameter(description = OpenApiDocs.IDEMPOTENCY_KEY_DESCRIPTION,
                    example = OpenApiDocs.IDEMPOTENCY_KEY_EXAMPLE, required = true) String rawKey,
            @Valid @RequestBody TransferRequest body) {

        IdempotencyKey idempotencyKey = IdempotencyKey.create(rawKey);

        TransferMoneyResult result = transferMoney.handle(new TransferMoneyCommand(
                idempotencyKey,
                RequestHasher.sha256(body.canonicalForm()),
                AccountId.of(body.sourceAccountId()),
                AccountId.of(body.destinationAccountId()),
                body.amount(),
                body.currency().toUpperCase(Locale.ROOT),
                body.reference()));

        String baseUrl = ApiPaths.baseUrl();
        return ApiResponses.createdOrReplayed(
                assembler.toResource(result, baseUrl),
                URI.create(ApiPaths.transaction(baseUrl, result.transactionId())),
                result.replayed());
    }
}
