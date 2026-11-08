package catalogue.domain;

import catalogue.domain.exception.InvalidShelfOrderException;
import catalogue.domain.exception.ShelfTitleRequiredException;
import java.util.Objects;

/** A homepage shelf curated by Homa, such as "Weekend quick refresh", driven by a preset search. */
public final class Shelf {

    private final String id;
    private final String title;
    private final PresetSearch preset;
    private final int order;

    private Shelf(String id, String title, PresetSearch preset, int order) {
        this.id = id;
        this.title = title;
        this.preset = preset;
        this.order = order;
    }

    /**
     * @param order the shelf's place on the homepage, from 1 for the first shelf
     */
    public static Shelf create(String title, PresetSearch preset, int order) {
        if (title == null || title.isBlank()) {
            throw new ShelfTitleRequiredException();
        }
        if (order < 1) {
            throw new InvalidShelfOrderException(order);
        }
        return new Shelf(new ShelfId().generate(), title.strip(), Objects.requireNonNull(preset), order);
    }

    public String id() {
        return id;
    }

    public String title() {
        return title;
    }

    public PresetSearch preset() {
        return preset;
    }

    public int order() {
        return order;
    }
}
