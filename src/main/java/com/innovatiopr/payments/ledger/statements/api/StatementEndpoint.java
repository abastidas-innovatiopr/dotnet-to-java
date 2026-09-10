package com.innovatiopr.payments.ledger.statements.api;

import com.innovatiopr.payments.accounts.AccountId;
import com.innovatiopr.payments.ledger.statements.application.GetAccountStatementHandler;
import com.innovatiopr.payments.ledger.statements.application.GetAccountStatementQuery;
import com.innovatiopr.payments.ledger.statements.application.StatementLine;
import com.innovatiopr.payments.shared.api.ApiPaths;
import com.innovatiopr.payments.shared.api.ApiResponses;
import com.innovatiopr.payments.shared.api.PageParams;
import com.innovatiopr.payments.shared.api.PagedResources;
import com.innovatiopr.payments.shared.application.PageRequest;
import com.innovatiopr.payments.shared.application.PageResult;
import com.innovatiopr.payments.shared.domain.Result;
import org.springframework.hateoas.MediaTypes;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.List;
import java.util.UUID;

@Component
class StatementEndpoint {

    private final GetAccountStatementHandler getStatement;
    private final StatementResourceAssembler assembler;

    StatementEndpoint(GetAccountStatementHandler getStatement, StatementResourceAssembler assembler) {
        this.getStatement = getStatement;
        this.assembler = assembler;
    }

    ServerResponse getStatement(ServerRequest request) {
        UUID accountId;
        try {
            accountId = UUID.fromString(request.pathVariable("accountId"));
        } catch (IllegalArgumentException e) {
            return ApiResponses.invalidIdentifier("account id", request.pathVariable("accountId"));
        }

        PageRequest page = PageParams.from(request, "recordedAt");
        Result<PageResult<StatementLine>> result = getStatement.handle(new GetAccountStatementQuery(
                AccountId.of(accountId),
                PageParams.instantParam(request, "from").orElse(null),
                PageParams.instantParam(request, "to").orElse(null),
                page));

        String baseUrl = ApiPaths.baseUrl(request);
        return result.fold(
                found -> {
                    List<StatementLineResource> resources = found.items().stream()
                            .map(line -> assembler.toResource(line, baseUrl))
                            .toList();
                    return ServerResponse.ok()
                            .contentType(MediaTypes.HAL_JSON)
                            .body(PagedResources.of(found, resources, request,
                                    ApiPaths.accountStatement(baseUrl, accountId)));
                },
                ApiResponses::problem);
    }
}
