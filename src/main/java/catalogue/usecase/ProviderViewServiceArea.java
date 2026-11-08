package catalogue.usecase;

import catalogue.domain.City;
import catalogue.usecase.dto.ViewServiceAreaRequest;
import catalogue.usecase.dto.ViewServiceAreaResponse;
import catalogue.usecase.exception.ServiceAreaNotLaunchedException;
import catalogue.usecase.port.in.ProviderServiceAreaViewer;
import catalogue.usecase.port.out.ServiceAreaRepository;
import java.util.stream.Collectors;

/** Shows the cities a provider can cover: the cities Homa serves. */
public final class ProviderViewServiceArea implements ProviderServiceAreaViewer {

    private final ServiceAreaRepository areas;

    public ProviderViewServiceArea(ServiceAreaRepository areas) {
        this.areas = areas;
    }

    @Override
    public ViewServiceAreaResponse view(ViewServiceAreaRequest request) {
        return new ViewServiceAreaResponse(areas.load()
                .orElseThrow(ServiceAreaNotLaunchedException::new)
                .cities().stream()
                .map(City::name)
                .collect(Collectors.toSet()));
    }
}
