package catalogue.usecase;

import catalogue.usecase.dto.ViewShowcaseResponse;
import catalogue.usecase.dto.ViewShowcaseResponse.Search;
import catalogue.usecase.port.in.GuestShowcaseViewer;
import catalogue.usecase.port.out.ShowcaseRepository;

public final class GuestViewShowcase implements GuestShowcaseViewer {

    private final ShowcaseRepository showcases;

    public GuestViewShowcase(ShowcaseRepository showcases) {
        this.showcases = showcases;
    }

    @Override
    public ViewShowcaseResponse view() {
        return new ViewShowcaseResponse(showcases.get().shown().stream()
                .map(search -> new Search(search.id(), search.name(), search.jobIds(), search.hours(), search.helpers()))
                .toList());
    }
}
