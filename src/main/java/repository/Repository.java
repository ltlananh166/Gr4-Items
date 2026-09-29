package repository;

import java.util.List;
import java.util.Optional;
import model.BaseEntity;

public interface Repository<T extends BaseEntity> {
    List<T> getAll();

    void saveAll(List<T> items);

    default Optional<T> findById(String id) {
        return getAll().stream()
                .filter(item -> item.getId().equals(id))
                .findFirst();
    }

    default void save(T item) {
        List<T> items = getAll();
        items.removeIf(existing -> existing.getId().equals(item.getId()));
        items.add(item);
        saveAll(items);
    }

    default void update(T item) {
        save(item);
    }
}