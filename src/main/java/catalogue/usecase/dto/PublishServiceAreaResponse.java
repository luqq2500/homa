package catalogue.usecase.dto;

import java.util.List;

/** The cities Homa serves, in alphabetical order. */
public record PublishServiceAreaResponse(List<String> cities) {

    public PublishServiceAreaResponse {
        cities = List.copyOf(cities);
    }
}
