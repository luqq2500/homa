package catalogue.usecase;

import catalogue.domain.FeaturedSearch;
import catalogue.domain.JobCode;
import catalogue.domain.Showcase;
import catalogue.usecase.dto.CustomerViewShowcaseResponse;
import catalogue.usecase.port.in.CustomerShowcaseViewer;
import catalogue.usecase.port.out.ShowcaseRepository;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Shows a customer the featured searches on the homepage, in the order Homa set. One left with no job is not
 * shown.
 */
public final class CustomerViewShowcase implements CustomerShowcaseViewer {

    private final ShowcaseRepository showcases;

    public CustomerViewShowcase(ShowcaseRepository showcases) {
        this.showcases = showcases;
    }

    @Override
    public CustomerViewShowcaseResponse view() {
        return new CustomerViewShowcaseResponse(showcases.find().map(Showcase::featuredSearches).orElse(List.of()).stream()
                .filter(FeaturedSearch::isShown)
                .map(CustomerViewShowcase::toView)
                .toList());
    }

    private static CustomerViewShowcaseResponse.FeaturedSearch toView(FeaturedSearch search) {
        return new CustomerViewShowcaseResponse.FeaturedSearch(
                search.id(),
                search.title(),
                search.preset().jobs().stream().map(JobCode::value).collect(Collectors.toSet()),
                search.preset().hours(),
                search.preset().helpers());
    }
}
