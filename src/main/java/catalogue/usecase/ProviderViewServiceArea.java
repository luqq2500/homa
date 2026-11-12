package catalogue.usecase;

import catalogue.domain.City;
import catalogue.domain.ServiceArea;
import catalogue.usecase.dto.ProviderViewServiceAreaResponse;
import catalogue.usecase.exception.ServiceAreaNotPublishedException;
import catalogue.usecase.port.in.ProviderServiceAreaViewer;
import catalogue.usecase.port.out.ServiceAreaRepository;

/** Shows a provider the cities they can cover. */
public final class ProviderViewServiceArea implements ProviderServiceAreaViewer {

    private final ServiceAreaRepository serviceAreas;

    public ProviderViewServiceArea(ServiceAreaRepository serviceAreas) {
        this.serviceAreas = serviceAreas;
    }

    @Override
    public ProviderViewServiceAreaResponse view() {
        ServiceArea area = serviceAreas.find().orElseThrow(ServiceAreaNotPublishedException::new);
        return new ProviderViewServiceAreaResponse(area.cities().stream().map(City::name).sorted().toList());
    }
}
