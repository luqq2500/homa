package catalogue.usecase.dto;

import java.util.List;

/** Every shelf on the homepage, in its new order. */
public record MoveShelfResponse(List<String> shelfIds) {

    public MoveShelfResponse {
        shelfIds = List.copyOf(shelfIds);
    }
}
