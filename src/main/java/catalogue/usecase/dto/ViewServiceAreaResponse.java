package catalogue.usecase.dto;

import java.util.Set;

/** The cities Homa serves. */
public record ViewServiceAreaResponse(Set<String> cities) {

    public ViewServiceAreaResponse {
        cities = Set.copyOf(cities);
    }
}
