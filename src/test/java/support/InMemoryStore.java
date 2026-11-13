package support;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/** A repository fake: keeps what is saved, in the order first saved, and counts the saves so tests can check them. */
public class InMemoryStore<T> {

    private final Function<T, String> idOf;
    private final Map<String, T> saved = new LinkedHashMap<>();
    private final List<String> saves = new ArrayList<>();

    protected InMemoryStore(Function<T, String> idOf) {
        this.idOf = idOf;
    }

    public void save(T item) {
        saved.put(idOf.apply(item), item);
        saves.add(idOf.apply(item));
    }

    public Optional<T> find(String id) {
        return Optional.ofNullable(saved.get(id));
    }

    public List<T> all() {
        return List.copyOf(saved.values());
    }

    /** How many times the item with this id was saved. */
    public int timesSaved(String id) {
        return (int) saves.stream().filter(id::equals).count();
    }

    public int saves() {
        return saves.size();
    }
}
