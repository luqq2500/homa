package catalogue.usecase;

import catalogue.domain.Showcase;
import catalogue.usecase.dto.MoveFeaturedSearchRequest;
import catalogue.usecase.dto.MoveFeaturedSearchResponse;
import catalogue.usecase.port.in.AdminFeaturedSearchMover;
import catalogue.usecase.port.out.ShowcaseRepository;

public final class AdminMoveFeaturedSearch implements AdminFeaturedSearchMover {

    private final ShowcaseRepository showcases;

    public AdminMoveFeaturedSearch(ShowcaseRepository showcases) {
        this.showcases = showcases;
    }

    @Override
    public MoveFeaturedSearchResponse move(MoveFeaturedSearchRequest request) {
        Showcase showcase = showcases.get();
        showcase.move(request.searchId(), request.place());
        showcases.save(showcase);
        return new MoveFeaturedSearchResponse(request.searchId(), showcase.placeOf(request.searchId()));
    }
}
