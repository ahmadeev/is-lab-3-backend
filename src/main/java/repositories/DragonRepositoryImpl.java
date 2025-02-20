package repositories;

import jakarta.data.Order;
import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import objects.Dragon;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;


@ApplicationScoped
public class DragonRepositoryImpl implements DragonRepository {

    @PersistenceContext
    private EntityManager entityManager;


    @Override
    public <S extends Dragon> S insert(S entity) {
        return null;
    }

    @Override
    public <S extends Dragon> List<S> insertAll(List<S> entities) {
        return List.of();
    }

    @Override
    public <S extends Dragon> S update(S entity) {
        return null;
    }

    @Override
    public <S extends Dragon> List<S> updateAll(List<S> entities) {
        return List.of();
    }

    @Override
    public <S extends Dragon> S save(S entity) {
        return null;
    }

    // TODO: туду
    @Override
    public <S extends Dragon> List<S> saveAll(List<S> entities) {
        for (Dragon dragon : entities) {
            entityManager.persist(dragon);
        }
        return entities;
    }

    @Override
    public Optional<Dragon> findById(Long id) {
        return Optional.empty();
    }

    @Override
    public Stream<Dragon> findAll() {
        return Stream.empty();
    }

    @Override
    public Page<Dragon> findAll(PageRequest pageRequest, Order<Dragon> sortBy) {
        return null;
    }

    @Override
    public void deleteById(Long id) {

    }

    @Override
    public void delete(Dragon entity) {

    }

    @Override
    public void deleteAll(List<? extends Dragon> entities) {

    }
}
