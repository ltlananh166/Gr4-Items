package repository;

import java.util.List;
import java.util.Optional;
import model.BaseEntity;

public interface Repository<T extends BaseEntity> {
    public List<T> getAll();

    public void saveAll(List<T> items);

    public default Optional<T> findById(String id) {
        return getAll().stream()
                .filter(item -> item.getId().equals(id))
                .findFirst();
    }

    public default void save(T item) {
        List<T> items = getAll();
        items.removeIf(existing -> existing.getId().equals(item.getId()));
        items.add(item);
        saveAll(items);
    }

    public default void update(T item) {
        save(item);
    }
}