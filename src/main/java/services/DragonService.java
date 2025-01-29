package services;

import dto.DragonDTO;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import objects.*;
import jakarta.persistence.Query;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Named(value = "mainService")
@ApplicationScoped
public class DragonService {

    @PersistenceContext
    protected EntityManager em;

    @PostConstruct
    private void init() {
        System.out.println("DragonService initialized");
    }

    @Transactional
    public Dragon createDragon(Dragon dragon, long userId) {
        dragon.setOwnerId(userId);
        em.persist(dragon);
        return dragon;
    }

    @Transactional
    public Dragon getDragonById(long id) {
        return em.find(Dragon.class, id);
    }

    @Transactional
    public List<Dragon> getDragons(int page, int pageSize, String filterValue, String filterCol, String sortBy, String sortDir) {
        Map<String, String> columnNames = Map.ofEntries(
                Map.entry("Name", "d.name"),
                Map.entry("Coordinates: x", "c.x"),
                Map.entry("Coordinates: y", "c.y"),
                Map.entry("Cave: number of treasures", "dc.numberOfTreasures"),
                Map.entry("Killer: name", "p.name"),
                Map.entry("Killer: eye color", "p.eyeColor"),
                Map.entry("Killer: hair color", "p.hairColor"),
                Map.entry("Killer: Location: x", "l.x"),
                Map.entry("Killer: Location: y", "l.y"),
                Map.entry("Killer: Location: z", "l.z"),
                Map.entry("Killer: birthday", "p.birthday"),
                Map.entry("Killer: height", "p.height"),
                Map.entry("Age", "d.age"),
                Map.entry("Description", "d.description"),
                Map.entry("Wingspan", "d.wingspan"),
                Map.entry("Character", "d.character"),
                Map.entry("Head: eyes count", "dh.eyeCount"),
                Map.entry("Head: tooth count", "dh.toothCount")
        );


        // Базовый JPQL-запрос
        String baseQuery = """
        SELECT d
        FROM Dragon d
        LEFT JOIN FETCH d.coordinates c
        LEFT JOIN FETCH d.cave dc
        LEFT JOIN FETCH d.killer p
        LEFT JOIN FETCH d.head dh
        LEFT JOIN FETCH p.location l
        WHERE 1=1
        """;

        // Фильтрация
        boolean hasFilter = false;
        if (filterCol != null && !filterCol.isEmpty() && filterValue != null && !filterValue.isEmpty()) {
            baseQuery += " AND LOWER(" + columnNames.get(filterCol) + ") LIKE LOWER(:filterValue)";
            hasFilter = true;
        }

        // Сортировка
        if (sortDir != null && !sortDir.isEmpty()) {
            baseQuery += " ORDER BY " + columnNames.getOrDefault(sortBy, "d.id") + " " + sortDir.toUpperCase();
        } else {
            baseQuery += " ORDER BY " + columnNames.getOrDefault(sortBy, "d.id") + "ASC";
        }

        // Создание TypedQuery
        TypedQuery<Dragon> query = em.createQuery(baseQuery, Dragon.class);

        // Применение фильтра
        if (hasFilter) {
            query.setParameter("filterValue", "%" + filterValue + "%");
        }

        // Пагинация
        query.setFirstResult(page * pageSize);
        query.setMaxResults(pageSize);

        // Выполнение запроса
        return query.getResultList();

//        return em.createQuery("SELECT i FROM Dragon i", Dragon.class)
//                .setFirstResult(page * size)
//                .setMaxResults(size)
//                .getResultList();
    }

    //  пока не готово
    @Transactional
    public boolean updateDragonById(long id, long userId, DragonDTO dragonDTO) {
        Dragon dragon = em.find(Dragon.class, id);

        if (dragon == null) return false;
        if (dragon.getOwnerId() != userId) return false;

        // Обновление полей
        dragon.setName(dragonDTO.getName());
        dragon.setAge(dragonDTO.getAge());
        dragon.setWingspan(dragonDTO.getWingspan());
        dragon.setDescription(dragonDTO.getDescription());

        return true;
    }

    @Transactional
    public boolean deleteDragonById(long id, long userId) {
        Dragon dragon = em.find(Dragon.class, id);

        if (dragon == null) return false;
        if (dragon.getOwnerId() != userId) return false;

        em.remove(dragon);
        return true;
    }

    @Transactional
    public int deleteDragons(long userId) {
        String jpql = "DELETE FROM Dragon o WHERE o.ownerId = :userId";
        Query query = em.createQuery(jpql);
        query.setParameter("userId", userId);
        return query.executeUpdate();
    }

    // ---------------- вспомогательные функции

    public List<Coordinates> getCoordinates() {
        return em.createQuery("SELECT e FROM Coordinates e", Coordinates.class).getResultList();
    }

    public List<DragonCave> getDragonCave() {
        return em.createQuery("SELECT e FROM DragonCave e", DragonCave.class).getResultList();
    }

    public List<Person> getPerson() {
        return em.createQuery("SELECT e FROM Person e", Person.class).getResultList();
    }

    public List<DragonHead> getDragonHead() {
        return em.createQuery("SELECT e FROM DragonHead e", DragonHead.class).getResultList();
    }

    public Dragon createEntityFromDTO(DragonDTO dto) {
        var coordinates = dto.getCoordinates();
        var cave = dto.getCave();
        var head = dto.getHead();

        if (coordinates == null || cave == null) return null;

        var killer = dto.getKiller();

        if (killer == null) return null;

        var location = killer.getLocation();

        return new Dragon(
                // без id, creationTime и ownerId
                dto.getName(),
                new Coordinates(
                        coordinates.getX(),
                        coordinates.getY()
                ),
                new DragonCave(
                        cave.getNumberOfTreasures()
                ),
                new Person(
                        killer.getName(),
                        killer.getEyeColor(),
                        killer.getHairColor(),
                        new Location(
                                location.getX(),
                                location.getY(),
                                location.getZ()
                        ),
                        killer.getBirthday(),
                        killer.getHeight()
                ),
                dto.getAge(),
                dto.getDescription(),
                dto.getWingspan(),
                dto.getCharacter(),
                new DragonHead(
                        head.getEyesCount(),
                        head.getToothCount()
                )
        );
    }

    // ---------------- дополнительные функции

    public void fun1() {
        return;
    }
}
