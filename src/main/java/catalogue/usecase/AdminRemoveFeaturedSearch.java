package catalogue.usecase;

import catalogue.domain.Showcase;
import catalogue.usecase.dto.RemoveFeaturedSearchRequest;
import catalogue.usecase.dto.RemoveFeaturedSearchResponse;
import catalogue.usecase.port.in.AdminFeaturedSearchRemover;
import catalogue.usecase.port.out.ShowcaseRepository;

public final class AdminRemoveFeaturedSearch implements AdminFeaturedSearchRemover {

    private final ShowcaseRepository showcases;

    public AdminRemoveFeaturedSearch(ShowcaseRepository showcases) {
        this.showcases = showcases;
    }

    @Override
    public RemoveFeaturedSearchResponse remove(RemoveFeaturedSearchRequest request) {
        Showcase showcase = showcases.get();
        showcase.remove(request.searchId());
        showcases.save(showcase);
        return new RemoveFeaturedSearchResponse(request.searchId());
    }
}
