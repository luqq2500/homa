package catalogue.usecase.dto;

import java.util.List;

/** Every shelf left on the homepage, in order. */
public record RemoveShelfResponse(List<String> shelfIds) {

    public RemoveShelfResponse {
        shelfIds = List.copyOf(shelfIds);
    }
}
