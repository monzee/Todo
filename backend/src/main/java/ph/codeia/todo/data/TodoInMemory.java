package ph.codeia.todo.data;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A very poor man's database table.
 */
public class TodoInMemory implements TodoRepository.Transactional, Serializable {
    private static final long serialVersionUID = 1L;

    private final AtomicInteger counter = new AtomicInteger(1);
    private final List<Todo> data = new ArrayList<>();
    private final Map<Integer, Integer> byId = new TreeMap<>();
    private final Queue<Integer> holes = new ArrayDeque<>();

    /**
     * O(log n)
     *
     * @param id The row id.
     * @return null if absent.
     */
    @Override
    public Todo oneWithId(int id) {
        if (byId.containsKey(id)) {
            return data.get(byId.get(id));
        }
        return null;
    }

    /**
     * O(n)
     *
     * @return all items sorted by id.
     */
    @Override
    public List<Todo> all() {
        List<Todo> items = new ArrayList<>();
        for (int index : byId.values()) {
            items.add(data.get(index));
        }
        return items;
    }

    /**
     * O(log n)
     *
     * @param title
     * @param description
     * @param completed
     * @return a tuple with an auto-assigned id guaranteed to be unique.
     */
    @Override
    public synchronized Todo add(String title, String description, boolean completed) {
        Todo item = new Todo(nextId(), title, description, completed, new Date());
        add(item);
        return item;
    }

    /**
     * O(log n)
     *
     * @param item The tuple to save.
     */
    @Override
    public synchronized void put(Todo item) {
        if (byId.containsKey(item.id)) {
            data.set(byId.get(item.id), item);
        } else {
            add(item);
        }
    }

    /**
     * O(log n)
     *
     * @param id
     */
    @Override
    public synchronized void delete(int id) {
        if (byId.containsKey(id)) {
            data.set(byId.get(id), null);
            holes.add(byId.remove(id));
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public TodoInMemory transact() {
        return this;
    }


    @Override
    public void cancel() {
    }

    @Override
    public void close() {
    }

    /**
     * O(log n)
     *
     * @param item The tuple to add.
     */
    private synchronized void add(Todo item) {
        int i = nextIndex();
        byId.put(item.id, i);
        data.set(i, item);
    }

    /**
     * O(1)
     *
     * @return the smallest index available in the list.
     */
    private synchronized int nextIndex() {
        if (!holes.isEmpty()) {
            return holes.remove();
        }
        data.add(null);
        return data.size() - 1;
    }

    /**
     * O(log n)
     *
     * This is used by {@link #add(String, String, boolean)} and since
     * {@link #add(Todo)} (which it calls eventually) is O(log n) anyway, might
     * as well make the id generation O(log n). This would make it ok to put
     * entities with any id that isn't already taken.
     *
     * @return an id guaranteed to be unique.
     */
    private int nextId() {
        int id;
        synchronized (byId) {
            do {
                id = counter.getAndIncrement();
            } while (byId.containsKey(id));
        }
        return id;
    }

}
