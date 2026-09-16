package com.innovatiopr.payments.ledger.statements.api;

import com.innovatiopr.payments.accounts.AccountId;
import com.innovatiopr.payments.ledger.statements.application.GetAccountStatementHandler;
import com.innovatiopr.payments.ledger.statements.application.GetAccountStatementQuery;
import com.innovatiopr.payments.ledger.statements.application.StatementLine;
import com.innovatiopr.payments.shared.api.ApiPaths;
import com.innovatiopr.payments.shared.api.PageQuery;
import com.innovatiopr.payments.shared.api.PagedResources;
import com.innovatiopr.payments.shared.api.QueryValues;
import com.innovatiopr.payments.shared.api.docs.OpenApiDocs;
import com.innovatiopr.payments.shared.application.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(path = ApiPaths.ACCOUNTS, produces = MediaTypes.HAL_JSON_VALUE)
@Tag(name = OpenApiDocs.TAG_LEDGER)
public class StatementController {

    private static final String DEFAULT_SORT = "recordedAt";

    private final GetAccountStatementHandler getStatement;
    private final StatementResourceAssembler assembler;

    StatementController(GetAccountStatementHandler getStatement, StatementResourceAssembler assembler) {
        this.getStatement = getStatement;
        this.assembler = assembler;
    }

    @GetMapping("/{accountId}/statement")
    @Operation(
            operationId = "getAccountStatement",
            summary = "Fetch an account statement",
            description = """
                    The auditable ledger view: one line per double-entry posting against this account.

                    Each line carries a `runningBalance` computed by a SQL **window function** over the \
                    account's whole history — it cannot be derived from the page alone, since page \
                    three still needs the balance carried forward from every earlier posting. The \
                    newest line's running balance always equals the account balance.

                    This is the clearest case in the codebase for `JdbcClient` over JPA: through the \
                    write model the same answer would mean loading every ledger entry into memory on \
                    every request.

                    Sortable fields: `recordedAt`, `amount`. Unknown values fall back to `recordedAt`.""")
    @ApiResponse(responseCode = "200", description = "A page of statement lines, newest first.")
    @ApiResponse(responseCode = "404", description = "`ACCOUNT_NOT_FOUND`.")
    public PagedModel<StatementLineResource> getStatement(
            @PathVariable @Parameter(description = OpenApiDocs.ACCOUNT_ID_DESCRIPTION) UUID accountId,
            @RequestParam(required = false)
            @Parameter(description = "Inclusive lower bound on `recordedAt`, as an ISO-8601 instant.",
                    example = "2026-01-01T00:00:00Z") String from,
            @RequestParam(required = false)
            @Parameter(description = "Inclusive upper bound on `recordedAt`, as an ISO-8601 instant.",
                    example = "2026-12-31T23:59:59Z") String to,
            PageQuery page) {

        PageResult<StatementLine> found = getStatement.handle(new GetAccountStatementQuery(
                AccountId.of(accountId),
                QueryValues.instant(from).orElse(null),
                QueryValues.instant(to).orElse(null),
                page.toPageRequest(DEFAULT_SORT)));

        String baseUrl = ApiPaths.baseUrl();
        List<StatementLineResource> resources = found.items().stream()
                .map(line -> assembler.toResource(line, baseUrl))
                .toList();
        return PagedResources.of(found, resources, ApiPaths.accountStatement(baseUrl, accountId));
    }
}
