package catalogue.domain;

import catalogue.domain.exception.InvalidShelfOrderException;
import catalogue.domain.exception.ShelfAlreadyOnHomepageException;
import catalogue.domain.exception.ShelfNotOnHomepageException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The order of the shelves on the homepage. Positions run from 1, the first shelf, with no gaps and no two
 * shelves in the same place: placing or moving a shelf moves the others along.
 */
public final class Homepage {

    private final String id;
    private final List<String> shelfIds = new ArrayList<>();

    private Homepage(String id) {
        this.id = id;
    }

    public static Homepage create() {
        return new Homepage(new HomepageId().generate());
    }

    /**
     * Places a new shelf at {@code position}, from 1 to one past the last shelf. The shelves from there on
     * move down one.
     */
    public void place(String shelfId, int position) {
        Objects.requireNonNull(shelfId);
        if (shelfIds.contains(shelfId)) {
            throw new ShelfAlreadyOnHomepageException(shelfId);
        }
        requirePosition(position, shelfIds.size() + 1);
        shelfIds.add(position - 1, shelfId);
    }

    /**
     * Moves a shelf to {@code position}, from 1 to the last shelf; the shelves in between move along.
     * Returns whether it moved: its own position changes nothing.
     */
    public boolean move(String shelfId, int position) {
        int from = positionOf(shelfId);
        requirePosition(position, shelfIds.size());
        shelfIds.remove(from - 1);
        shelfIds.add(position - 1, shelfId);
        return from != position;
    }

    /** Takes a shelf off the homepage; the shelves after it move up one. */
    public void remove(String shelfId) {
        shelfIds.remove(positionOf(shelfId) - 1);
    }

    /** Where the shelf sits, from 1 for the first shelf. */
    public int positionOf(String shelfId) {
        int index = shelfIds.indexOf(shelfId);
        if (index < 0) {
            throw new ShelfNotOnHomepageException(shelfId);
        }
        return index + 1;
    }

    /** The shelves in the order customers see them. */
    public List<String> shelfIds() {
        return List.copyOf(shelfIds);
    }

    public String id() {
        return id;
    }

    private static void requirePosition(int position, int last) {
        if (position < 1 || position > last) {
            throw new InvalidShelfOrderException(position, last);
        }
    }
}
