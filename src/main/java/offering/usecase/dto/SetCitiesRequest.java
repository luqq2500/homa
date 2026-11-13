package offering.usecase.dto;

import java.util.Set;
import kernel.City;

public record SetCitiesRequest(String providerId, Set<City> cities) {

    public SetCitiesRequest {
        cities = Set.copyOf(cities);
    }
}
