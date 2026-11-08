package catalogue.usecase.dto;

import java.util.Set;

/** The cities Homa now serves. */
public record LaunchServiceAreaResponse(Set<String> cities) {

    public LaunchServiceAreaResponse {
        cities = Set.copyOf(cities);
    }
}
