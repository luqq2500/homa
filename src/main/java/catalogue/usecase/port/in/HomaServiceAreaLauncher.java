package catalogue.usecase.port.in;

import catalogue.usecase.dto.LaunchServiceAreaRequest;
import catalogue.usecase.dto.LaunchServiceAreaResponse;

/** Homa launches its service area. */
public interface HomaServiceAreaLauncher {

    LaunchServiceAreaResponse launch(LaunchServiceAreaRequest request);
}
