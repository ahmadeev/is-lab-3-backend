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
    public Dragon createDragon(DragonDTO dto, long userId) {

        Dragon dragon = createEntityFromDTO(dto);

        // можно проверять и на null, но смысла мало
        if (dto.getCoordinates().getId() != -1) {
            Coordinates detachedCoordinates = em.find(Coordinates.class, dto.getCoordinates().getId());

            if (detachedCoordinates == null) return null;

            Coordinates coordinates = em.merge(detachedCoordinates);

            dragon.setCoordinates(coordinates);
        }

        if (dto.getCave().getId() != -1) {
            DragonCave detachedDragonCave = em.find(DragonCave.class, dto.getCave().getId());

            if (detachedDragonCave == null) return null;

            DragonCave dragonCave = em.merge(detachedDragonCave);

            dragon.setCave(dragonCave);
        }

        if (dto.getKiller().getId() != -1) {
            Person detachedPerson = em.find(Person.class, dto.getKiller().getId());

            if (detachedPerson == null) return null;

            Person person = em.merge(detachedPerson);

            dragon.setKiller(person);
        }

        if (dto.getKiller().getLocation().getId() != -1) {
            Location detachedLocation = em.find(Location.class, dto.getKiller().getLocation().getId());

            if (detachedLocation == null) return null;

            Location location = em.merge(detachedLocation);

            dragon.getKiller().setLocation(location);
        }

        if (dto.getHead().getId() != -1) {
            DragonHead detachedDragonHead = em.find(DragonHead.class, dto.getHead().getId());

            if (detachedDragonHead == null) return null;

            DragonHead dragonHead = em.merge(detachedDragonHead);

            dragon.setHead(dragonHead);
        }

        dragon.setOwnerId(userId);
        System.out.println(dragon.toJson());
        em.persist(dragon);
        return dragon;
    }

    @Transactional
    public Dragon getDragonById(long id) {
        return em.find(Dragon.class, id);
    }

    @Transactional
    public List<Dragon> getDragons(int page, int pageSize, String filterValue, String filterCol, String sortBy, String sortDir) {
        // в будущем необходимо добавить id и ownerId
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
    }

    //  пока не готово
    @Transactional
    public boolean updateDragonById(long id, long userId, DragonDTO dto) {
        Dragon dragon = em.find(Dragon.class, id);

        // TODO: нужна более глубокая проверка ownerId
        if (dragon == null) return false;
        if (dragon.getOwnerId() != userId) return false;

        // можно проверять и на null, но смысла мало
        // 1) апдейтим с помощью уже существующих объектов (в таком случае старые просто подвисают), заменяя старый айди на новый
        // 2) апдейтим новыми значениями на тот же айди

        // проблема в том, что изначально (1) для замены одного объекта другим,
        // а (2) для обновления полей того же объекта
        // TODO: ??? выполнение блоков последовательно может привести к перезаписи полей чужих объектов

        // --- 1
        Coordinates detachedCoordinates = em.find(Coordinates.class, dto.getCoordinates().getId());
        Coordinates coordinates = em.merge(detachedCoordinates);
        dragon.setCoordinates(coordinates);
        // --- 2
        dragon.getCoordinates().setX(dto.getCoordinates().getX());
        dragon.getCoordinates().setY(dto.getCoordinates().getY());

        // --- 1
        DragonCave detachedDragonCave = em.find(DragonCave.class, dto.getCave().getId());
        DragonCave dragonCave = em.merge(detachedDragonCave);
        dragon.setCave(dragonCave);
        // --- 2
        dragon.getCave().setNumberOfTreasures(dto.getCave().getNumberOfTreasures());

        // --- 1
        Person detachedPerson = em.find(Person.class, dto.getKiller().getId());
        Person person = em.merge(detachedPerson);
        dragon.setKiller(person);
        // --- 2
        dragon.getKiller().setName(dto.getKiller().getName());
        dragon.getKiller().setEyeColor(dto.getKiller().getEyeColor());
        dragon.getKiller().setHairColor(dto.getKiller().getHairColor());
        // намеренно упускаем Location (обновим ниже)
        dragon.getKiller().setBirthday(dto.getKiller().getBirthday());
        dragon.getKiller().setHeight(dto.getKiller().getHeight());

        // --- 1
        Location detachedLocation = em.find(Location.class, dto.getKiller().getLocation().getId());
        Location location = em.merge(detachedLocation);
        dragon.getKiller().setLocation(location);
        // --- 2
        dragon.getKiller().getLocation().setX(dto.getKiller().getLocation().getX());
        dragon.getKiller().getLocation().setY(dto.getKiller().getLocation().getY());
        dragon.getKiller().getLocation().setZ(dto.getKiller().getLocation().getZ());

        // --- 1
        DragonHead detachedDragonHead = em.find(DragonHead.class, dto.getHead().getId());
        DragonHead dragonHead = em.merge(detachedDragonHead);
        dragon.setHead(dragonHead);
        // --- 2
        dragon.getCave().setNumberOfTreasures(dto.getCave().getNumberOfTreasures());

        // обновление обычных полей дракона
        dragon.setName(dto.getName());
        dragon.setAge(dto.getAge());
        dragon.setDescription(dto.getDescription());
        dragon.setWingspan(dto.getWingspan());
        dragon.setCharacter(dto.getCharacter());
        dragon.setOwnerId(userId);

        em.merge(dragon);
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

    public Object executeNativeQuery(String query) {
        return em.createNativeQuery(query).executeUpdate();
    }

    public Dragon createEntityFromDTO(DragonDTO dto) {
        var coordinates = dto.getCoordinates();
        var cave = dto.getCave();
        var head = dto.getHead();

        if (coordinates == null || cave == null) return null;

        var killer = dto.getKiller();

        // TODO: может быть киллер пустым??
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
