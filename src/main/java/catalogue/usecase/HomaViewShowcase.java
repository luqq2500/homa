package catalogue.usecase;

import catalogue.domain.FeaturedSearch;
import catalogue.domain.JobCode;
import catalogue.domain.Showcase;
import catalogue.usecase.dto.HomaViewShowcaseResponse;
import catalogue.usecase.port.in.HomaShowcaseViewer;
import catalogue.usecase.port.out.ShowcaseRepository;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Shows the founders every featured search in its place, including those no longer shown, so they can
 * replace them.
 */
public final class HomaViewShowcase implements HomaShowcaseViewer {

    private final ShowcaseRepository showcases;

    public HomaViewShowcase(ShowcaseRepository showcases) {
        this.showcases = showcases;
    }

    @Override
    public HomaViewShowcaseResponse view() {
        List<FeaturedSearch> inOrder = showcases.find().map(Showcase::featuredSearches).orElse(List.of());
        return new HomaViewShowcaseResponse(inOrder.stream()
                .map(search -> toView(search, inOrder.indexOf(search) + 1))
                .toList());
    }

    private static HomaViewShowcaseResponse.FeaturedSearch toView(FeaturedSearch search, int position) {
        return new HomaViewShowcaseResponse.FeaturedSearch(
                search.id(),
                search.title(),
                position,
                search.preset().jobs().stream().map(JobCode::value).collect(Collectors.toSet()),
                search.preset().hours(),
                search.preset().helpers(),
                search.isShown());
    }
}
