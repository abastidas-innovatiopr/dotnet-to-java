package com.innovatiopr.payments.shared.api;

import com.innovatiopr.payments.shared.application.PageResult;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
import org.springframework.util.MultiValueMap;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

/**
 * Builds a Spring HATEOAS {@link PagedModel} with correct navigation links.
 *
 * <h2>Links describe pages that exist</h2>
 * {@code prev} appears only when there is a previous page and {@code next} only when there is a next one.
 * Emitting both unconditionally is a common bug: a client following {@code next} off the end of the
 * collection gets an empty page and no way to know it has finished, which defeats the point of
 * hypermedia — the links are supposed to be the client's map of what it may do.
 *
 * <h2>Links preserve the query</h2>
 * Every generated URL carries the original filters, sort field and direction, with only {@code page}
 * replaced. Following {@code next} on a filtered, sorted collection must continue <em>that</em>
 * collection; dropping the filter would silently switch the client to a different result set halfway
 * through.
 *
 * <p>{@code size} is rewritten to the <em>effective</em> page size after clamping, so a client that asked
 * for 1000 and received 100 gets links that say 100 and can page consistently from there.
 */
public final class PagedResources {

    private PagedResources() {
    }

    public static <T> PagedModel<T> of(PageResult<?> result, List<T> content, String baseHref) {
        PagedModel.PageMetadata metadata = new PagedModel.PageMetadata(
                result.size(), result.page(), result.totalItems(), result.totalPages());

        PagedModel<T> model = PagedModel.of(content, metadata);
        model.add(Link.of(pageHref(baseHref, result.page(), result.size()), IanaLinkRelations.SELF));

        int lastPage = Math.max(result.totalPages() - 1, 0);
        if (result.totalPages() > 0) {
            model.add(Link.of(pageHref(baseHref, 0, result.size()), IanaLinkRelations.FIRST));
            model.add(Link.of(pageHref(baseHref, lastPage, result.size()), IanaLinkRelations.LAST));
        }
        if (result.hasPrevious()) {
            model.add(Link.of(pageHref(baseHref, result.page() - 1, result.size()),
                    IanaLinkRelations.PREV));
        }
        if (result.hasNext()) {
            model.add(Link.of(pageHref(baseHref, result.page() + 1, result.size()),
                    IanaLinkRelations.NEXT));
        }
        return model;
    }

    /** Rebuilds the request's query string with a new page number and the effective page size. */
    private static String pageHref(String baseHref, int page, int size) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseHref);
        currentQueryParams().forEach((name, values) -> {
            if (!"page".equals(name) && !"size".equals(name)) {
                values.forEach(value -> builder.queryParam(name, value));
            }
        });
        builder.queryParam("page", page);
        builder.queryParam("size", size);
        return builder.build().toUriString();
    }

    private static MultiValueMap<String, String> currentQueryParams() {
        return ServletUriComponentsBuilder.fromCurrentRequest().build().getQueryParams();
    }
}
