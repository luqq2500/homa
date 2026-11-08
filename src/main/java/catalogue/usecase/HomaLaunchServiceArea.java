package catalogue.usecase;

import catalogue.domain.City;
import catalogue.domain.ServiceArea;
import catalogue.usecase.dto.LaunchServiceAreaRequest;
import catalogue.usecase.dto.LaunchServiceAreaResponse;
import catalogue.usecase.exception.ServiceAreaAlreadyLaunchedException;
import catalogue.usecase.port.in.HomaServiceAreaLauncher;
import catalogue.usecase.port.out.ServiceAreaRepository;
import java.util.stream.Collectors;

/** Launches Homa's service area once: Salak Tinggi, Cyberjaya and Putrajaya. */
public final class HomaLaunchServiceArea implements HomaServiceAreaLauncher {

    private final ServiceAreaRepository areas;

    public HomaLaunchServiceArea(ServiceAreaRepository areas) {
        this.areas = areas;
    }

    @Override
    public LaunchServiceAreaResponse launch(LaunchServiceAreaRequest request) {
        if (areas.load().isPresent()) {
            throw new ServiceAreaAlreadyLaunchedException();
        }
        ServiceArea area = ServiceArea.launch();
        areas.save(area);
        return new LaunchServiceAreaResponse(area.cities().stream().map(City::name).collect(Collectors.toSet()));
    }
}
