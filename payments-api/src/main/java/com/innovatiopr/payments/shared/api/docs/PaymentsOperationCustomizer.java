package com.innovatiopr.payments.shared.api.docs;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;

import java.util.Map;

/**
 * Attaches the right media type to every documented response.
 *
 * <p>Success responses carry {@code application/hal+json} and failures carry
 * {@code application/problem+json}. Declaring that on each {@code @ApiResponse} would mean repeating a
 * {@code @Content} annotation roughly seventy times, and the first one anybody forgot would be a document
 * that quietly disagreed with the server. Stating it once here makes it structurally impossible to omit.
 *
 * <p>Error responses are <em>overwritten</em> rather than merely filled in. springdoc derives a response's
 * media type from the controller's {@code produces} attribute, which describes the success path, so
 * without this every 4xx would be documented as {@code application/hal+json} — a document that contradicts
 * what {@code ApiExceptionHandler} actually sends. Success responses are only filled in when absent, so an
 * operation can still declare something unusual.
 *
 * <p>It never invents a response the operation did not list. A blanket {@code 500} here would be
 * convenient and wrong — {@code OpenApiDocumentIT} asserts the transfer operation's response set is
 * exactly {@code {200, 201, 400, 404, 409, 422}}, and that assertion is worth more than the convenience.
 */
@Component
class PaymentsOperationCustomizer implements OperationCustomizer {

    private static final String HAL_JSON = "application/hal+json";
    private static final String PROBLEM_JSON = "application/problem+json";

    @Override
    public Operation customize(Operation operation, HandlerMethod handlerMethod) {
        if (operation.getResponses() == null) {
            return operation;
        }
        for (Map.Entry<String, ApiResponse> entry : operation.getResponses().entrySet()) {
            ApiResponse response = entry.getValue();
            if (isFailure(entry.getKey())) {
                response.setContent(new Content().addMediaType(PROBLEM_JSON, problemSchema()));
            } else if (response.getContent() == null || response.getContent().isEmpty()) {
                response.setContent(new Content().addMediaType(HAL_JSON, new MediaType()));
            }
        }
        return operation;
    }

    private static boolean isFailure(String statusCode) {
        return statusCode.startsWith("4") || statusCode.startsWith("5");
    }

    private static MediaType problemSchema() {
        return new MediaType().schema(new Schema<>().$ref("#/components/schemas/ProblemDetail"));
    }
}
