package catalogue.usecase;

import catalogue.domain.FeaturedSearch;
import catalogue.domain.Showcase;
import catalogue.usecase.dto.RenameFeaturedSearchRequest;
import catalogue.usecase.dto.RenameFeaturedSearchResponse;
import catalogue.usecase.port.in.AdminFeaturedSearchRenamer;
import catalogue.usecase.port.out.ShowcaseRepository;

public final class AdminRenameFeaturedSearch implements AdminFeaturedSearchRenamer {

    private final ShowcaseRepository showcases;

    public AdminRenameFeaturedSearch(ShowcaseRepository showcases) {
        this.showcases = showcases;
    }

    @Override
    public RenameFeaturedSearchResponse rename(RenameFeaturedSearchRequest request) {
        Showcase showcase = showcases.get();
        FeaturedSearch renamed = showcase.rename(request.searchId(), request.name());
        showcases.save(showcase);
        return new RenameFeaturedSearchResponse(renamed.id(), renamed.name());
    }
}
