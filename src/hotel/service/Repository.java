package hotel.service;

import hotel.model.Identifiable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A small in-memory store for anything with an ID.
 * Generic (T) so the same class holds both rooms and bookings without casting.
 */
public class Repository<T extends Identifiable> {
    // HashMap gives instant lookup by ID instead of a loop; the Linked version also keeps insertion order
    private final Map<Integer, T> items = new LinkedHashMap<>();

    /** Adds the item. Returns false, and changes nothing, if its ID is already taken. */
    public boolean add(T item) {
        return items.putIfAbsent(item.getId(), item) == null;
    }

    /** The item with this ID, or null if there is none. */
    public T findById(int id) { return items.get(id); }

    /** A copy of all items in the order they were added. */
    public List<T> getAll() { return new ArrayList<>(items.values()); }

    public int size() { return items.size(); }
}
