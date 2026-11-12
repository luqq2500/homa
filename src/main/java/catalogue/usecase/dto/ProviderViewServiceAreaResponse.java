package catalogue.usecase.dto;

import java.util.List;

/** The cities a provider can cover, in alphabetical order. */
public record ProviderViewServiceAreaResponse(List<String> cities) {

    public ProviderViewServiceAreaResponse {
        cities = List.copyOf(cities);
    }
}
