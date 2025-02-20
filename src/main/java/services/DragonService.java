package services;

import dto.*;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import jakarta.persistence.*;
import jakarta.transaction.Transactional;
import objects.*;
import utils.PairReturnBooleanString;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Named(value = "mainService")
@ApplicationScoped
public class DragonService {

/*    private final static int BATCH_SIZE = 50;*/

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
        // TODO: if стоит перевернуть (из-за этого были проблемы на лабе, но сейчас уже все хорошо)
        if (dto.getCoordinates().getId() != -1 && dto.getCoordinates().getId() != 0) {
            Coordinates detachedCoordinates = em.find(Coordinates.class, dto.getCoordinates().getId());

            if (detachedCoordinates == null) return null;

            Coordinates coordinates = em.merge(detachedCoordinates);

            dragon.setCoordinates(coordinates);
        } else if (dto.getCoordinates().getId() == -1 || dto.getCoordinates().getId() == 0) {
            dragon.getCoordinates().setOwnerId(userId);
            dragon.getCoordinates().setUpdatedBy(userId);
        }

        if (dto.getCave().getId() != -1 && dto.getCave().getId() != 0) {
            DragonCave detachedDragonCave = em.find(DragonCave.class, dto.getCave().getId());

            if (detachedDragonCave == null) return null;

            DragonCave dragonCave = em.merge(detachedDragonCave);

            dragon.setCave(dragonCave);
        } else if (dto.getCave().getId() == -1 || dto.getCave().getId() == 0) {
            dragon.getCave().setOwnerId(userId);
            dragon.getCave().setUpdatedBy(userId);
        }

        if (dto.getKiller() != null && dto.getKiller().getId() != -1 && dto.getKiller().getId() != 0) {
            Person detachedPerson = em.find(Person.class, dto.getKiller().getId());

            if (detachedPerson == null) return null;

            Person person = em.merge(detachedPerson);

            dragon.setKiller(person);
        } else if (dto.getKiller() != null && (dto.getKiller().getId() == -1 || dto.getKiller().getId() == 0)) {
            dragon.getKiller().setOwnerId(userId);
            dragon.getKiller().setUpdatedBy(userId);
        }

        if (dto.getKiller() != null && dto.getKiller().getLocation().getId() != -1 && dto.getKiller().getLocation().getId() != 0) {
            Location detachedLocation = em.find(Location.class, dto.getKiller().getLocation().getId());

            if (detachedLocation == null) return null;

            Location location = em.merge(detachedLocation);

            dragon.getKiller().setLocation(location);
        } else if (dto.getKiller() != null && (dto.getKiller().getLocation().getId() == -1 || dto.getKiller().getLocation().getId() == 0)) {
            dragon.getKiller().getLocation().setOwnerId(userId);
            dragon.getKiller().getLocation().setUpdatedBy(userId);
        }

        if (dto.getHead().getId() != -1 && dto.getHead().getId() != 0) {
            DragonHead detachedDragonHead = em.find(DragonHead.class, dto.getHead().getId());

            if (detachedDragonHead == null) return null;

            DragonHead dragonHead = em.merge(detachedDragonHead);

            dragon.setHead(dragonHead);
        } else if (dto.getHead().getId() == -1 || dto.getHead().getId() == 0) {
            dragon.getHead().setOwnerId(userId);
            dragon.getHead().setUpdatedBy(userId);
        }

        dragon.setOwnerId(userId);
        dragon.setUpdatedBy(userId);

        em.persist(dragon);
        return dragon;
    }

    @Transactional
    public List<Dragon> createAll(List<Dragon> dragons) {
        long start = System.nanoTime();
        for (Dragon dragon : dragons) {
            em.persist(dragon);
        }
        long end = System.nanoTime();
        System.out.println((end - start) / 1_000_000 + " ms");
        return dragons;
    }

/*    @Transactional
    public List<Dragon> createAll(List<Dragon> dragons) {
        long start = System.nanoTime();

        try {
            for (int i = 0; i < dragons.size(); i++) {
                em.persist(dragons.get(i));
            }
        } catch (Exception e) {
            throw e;
        }

        long end = System.nanoTime();
        System.out.println((end - start) / 1_000_000 + " ms");
        return dragons;
    }*/

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

        // столбцы, содержащие числовые значения
        Set<String> numericColumns = Set.of(
                "Coordinates: x",
                "Coordinates: y",
                "Cave: number of treasures",
                "Killer: Location: x",
                "Killer: Location: y",
                "Killer: Location: z",
                "Killer: height",
                "Age",
                "Wingspan",
                "Head: eyes count",
                "Head: tooth count"
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
            if (numericColumns.contains(filterCol)) {
                // точное
                baseQuery += " AND " + columnNames.get(filterCol) + " = :filterValue";
                // частичное
                // baseQuery += " AND FUNCTION('STR', " + columnNames.get(filterCol) + ") LIKE :filterValue"; // плохо
                // baseQuery += " AND CONCAT('', " + columnNames.get(filterCol) + ") LIKE :filterValue"; // плохо
            } else {
                baseQuery += " AND LOWER(%" + columnNames.get(filterCol) + "%) LIKE LOWER(:filterValue)";
            }
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
            query.setParameter("filterValue", filterValue);
        }

        // Пагинация
        query.setFirstResult(page * pageSize);
        query.setMaxResults(pageSize);

        // Выполнение запроса
        return query.getResultList();
    }

    @Transactional
    public boolean updateDragonById(long id, long userId, DragonDTO dto, boolean isUserAdmin) {
        Dragon dragon = em.find(Dragon.class, id);
        Dragon dragonToMerge = createEntityFromDTOAllArgs(dto);

        // TODO: осталось добавить обработку галочки на разрешение редактирования
        if (dragon == null) return false;
        if (!(dragon.getOwnerId() == userId || isUserAdmin && dragon.isAllowEditing())) return false;

        // можно проверять и на null, но смысла мало
        // 1) апдейтим с помощью уже существующих объектов (в таком случае старые просто подвисают), заменяя старый айди на новый
        // 2) апдейтим новыми значениями на тот же айди

        System.out.println("=============== coordinates : " + dragon.getCoordinates().equals(dragonToMerge.getCoordinates()));

        // айди пользователя получен из jwt
        // один и тот же объект (айди совпадают:
        //     один айди получен от пользователя из path (какой?),
        //     второй тоже -- но иначе -- отправлен в теле запроса на основании выбора объекта (чем?))
        // разные наборы полей (существующий объект в базе (какие?) и запрос на изменение (чем?))
        if (dragon.getCoordinates().getId() == dragonToMerge.getCoordinates().getId()
                && !(dragon.getCoordinates().equals(dragonToMerge.getCoordinates()))) {
            if (!(dragon.getCoordinates().getOwnerId() == userId || isUserAdmin && dragon.getCoordinates().isAllowEditing())) return false;
            // --- 2 --- обновляем предложенными изменениями
            dragon.getCoordinates().setX(dto.getCoordinates().getX());
            dragon.getCoordinates().setY(dto.getCoordinates().getY());
            dragon.getCoordinates().setUpdatedBy(userId);
        } else if (dragon.getCoordinates().getId() != dragonToMerge.getCoordinates().getId()) {
            if (!(dragon.getCoordinates().getOwnerId() == userId || isUserAdmin && dragon.getCoordinates().isAllowEditing())) return false;
            // --- 1 --- заменяем объект полученным из бд с помощью второго айди
            Coordinates coordinates = em.find(Coordinates.class, dto.getCoordinates().getId());
            dragon.setCoordinates(coordinates);
        }

        System.out.println("=============== cave ------ : " + dragon.getCave().equals(dragonToMerge.getCave()));

        if (dragon.getCave().getId() == dragonToMerge.getCave().getId()
                && !(dragon.getCave().equals(dragonToMerge.getCave()))) {
            if (!(dragon.getCave().getOwnerId() == userId || isUserAdmin && dragon.getCave().isAllowEditing())) return false;
            // --- 2
            dragon.getCave().setNumberOfTreasures(dto.getCave().getNumberOfTreasures());
            dragon.getCave().setUpdatedBy(userId);
        } else if (dragon.getCave().getId() != dragonToMerge.getCave().getId()) {
            if (!(dragon.getCave().getOwnerId() == userId || isUserAdmin && dragon.getCave().isAllowEditing())) return false;
            // --- 1
            DragonCave dragonCave = em.find(DragonCave.class, dto.getCave().getId());
            dragon.setCave(dragonCave);
        }

        if (dragon.getKiller() != null) {
            System.out.println("=============== killer ---- : " + dragon.getKiller().equals(dragonToMerge.getKiller()));
        }

        if (dragonToMerge.getKiller() == null) {
            dragon.setKiller(null);
        } else {
            if (dragon.getKiller().getId() == dragonToMerge.getKiller().getId()
                    && !(dragon.getKiller().equals(dragonToMerge.getKiller()))) {
                if (!(dragon.getKiller().getOwnerId() == userId || isUserAdmin && dragon.getKiller().isAllowEditing())) return false;
                // --- 2
                dragon.getKiller().setName(dto.getKiller().getName());
                dragon.getKiller().setEyeColor(dto.getKiller().getEyeColor());
                dragon.getKiller().setHairColor(dto.getKiller().getHairColor());
                // намеренно упускаем Location (обновим ниже)
                dragon.getKiller().setBirthday(dto.getKiller().getBirthday());
                dragon.getKiller().setHeight(dto.getKiller().getHeight());
                dragon.getKiller().setUpdatedBy(userId);
            } else if (dragon.getKiller().getId() != dragonToMerge.getKiller().getId()) {
                if (!(dragon.getKiller().getOwnerId() == userId || isUserAdmin && dragon.getKiller().isAllowEditing())) return false;
                // --- 1
                Person person = em.find(Person.class, dto.getKiller().getId());
                dragon.setKiller(person);
            }
        }

        if (dragon.getKiller() != null) {
            System.out.println("=============== location -- : " + dragon.getKiller().getLocation().equals(dragonToMerge.getKiller().getLocation()));

            if (dragon.getKiller().getLocation().getId() == dragonToMerge.getKiller().getLocation().getId()
                    && !(dragon.getKiller().getLocation().equals(dragonToMerge.getKiller().getLocation()))) {
                if (!(dragon.getKiller().getLocation().getOwnerId() == userId || isUserAdmin && dragon.getKiller().getLocation().isAllowEditing())) return false;
                // --- 2
                dragon.getKiller().getLocation().setX(dto.getKiller().getLocation().getX());
                dragon.getKiller().getLocation().setY(dto.getKiller().getLocation().getY());
                dragon.getKiller().getLocation().setZ(dto.getKiller().getLocation().getZ());
                dragon.getKiller().getLocation().setUpdatedBy(userId);
            } else if (dragon.getKiller().getLocation().getId() != dragonToMerge.getKiller().getLocation().getId()) {
                if (!(dragon.getKiller().getLocation().getOwnerId() == userId || isUserAdmin && dragon.getKiller().getLocation().isAllowEditing())) return false;
                // --- 1
                Location location = em.find(Location.class, dto.getKiller().getLocation().getId());
                dragon.getKiller().setLocation(location);
            }
        }

        System.out.println("=============== head ------ : " + dragon.getHead().equals(dragonToMerge.getHead()));

        if (dragon.getHead().getId() == dragonToMerge.getHead().getId()
                && !(dragon.getHead().equals(dragonToMerge.getHead()))) {
            if (!(dragon.getHead().getOwnerId() == userId || isUserAdmin && dragon.getHead().isAllowEditing())) return false;
            // --- 2
            dragon.getHead().setEyesCount(dto.getHead().getEyesCount());
            dragon.getHead().setToothCount(dto.getHead().getToothCount());
            dragon.getHead().setUpdatedBy(userId);
        } else if (dragon.getHead().getId() != dragonToMerge.getHead().getId()) {
            if (!(dragon.getHead().getOwnerId() == userId || isUserAdmin && dragon.getHead().isAllowEditing())) return false;
            // --- 1
            DragonHead dragonHead = em.find(DragonHead.class, dto.getHead().getId());
            dragon.setHead(dragonHead);
        }

        // обновление обычных полей дракона
        dragon.setName(dto.getName());
        dragon.setAge(dto.getAge());
        dragon.setDescription(dto.getDescription());
        dragon.setWingspan(dto.getWingspan());
        dragon.setCharacter(dto.getCharacter());
        dragon.setOwnerId(dragon.getOwnerId()); // TODO: подумать
        dragon.setUpdatedBy(userId);

        em.merge(dragon);
        return true;
    }

    @Transactional
    public PairReturnBooleanString deleteDragonById(long id, long userId, boolean isAdmin) {
        Dragon dragon = em.find(Dragon.class, id);

        if (dragon == null) {
            System.out.println("dragon does not exist");
            return new PairReturnBooleanString(false, "Dragon does not exist.");
        }
        if (dragon.getOwnerId() != userId) {
            System.out.println("you are not allowed to delete this dragon");
            return new PairReturnBooleanString(
                    false,
                    "You are not allowed to delete this dragon. You are not an owner."
            );
        }

        // проверять по собственным id вложенных объектов и проверять, есть ли dragon с такими же id вложенных объектов
        // эээээээээааааа ??? а нужно ли?

        em.remove(dragon);

        return new PairReturnBooleanString(true, "Successfully deleted dragon.");
    }

    // не каскадное
    @Transactional
    public int deleteDragons(long userId) {
        // здесь тоже проверять
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

    // без id, creationTime и ownerId
    public Dragon createEntityFromDTO(DragonDTO dto) {
        CoordinatesDTO coordinates = dto.getCoordinates();
        DragonCaveDTO cave = dto.getCave();
        DragonHeadDTO head = dto.getHead();

        if (coordinates == null || cave == null) return null;

        PersonDTO killer = dto.getKiller();
        LocationDTO location = null;

        if (killer != null) {
            location = killer.getLocation();
            if (location == null) return null;
        }

        return new Dragon(
                dto.getName(),
                new Coordinates(
                        coordinates.getX(),
                        coordinates.getY(),
                        coordinates.isAllowEditing()
                ),
                new DragonCave(
                        cave.getNumberOfTreasures(),
                        cave.isAllowEditing()
                ),
                killer == null ? null : new Person(
                        killer.getName(),
                        killer.getEyeColor(),
                        killer.getHairColor(),
                        new Location(
                                location.getX(),
                                location.getY(),
                                location.getZ(),
                                location.isAllowEditing()
                        ),
                        killer.getBirthday(),
                        killer.getHeight(),
                        killer.isAllowEditing()
                ),
                dto.getAge(),
                dto.getDescription(),
                dto.getWingspan(),
                dto.getCharacter(),
                new DragonHead(
                        head.getEyesCount(),
                        head.getToothCount(),
                        head.isAllowEditing()
                ),
                dto.isAllowEditing()
        );
    }

    // + id, ownerId и creation date
    public Dragon createEntityFromDTOAllArgs(DragonDTO dto) {
        CoordinatesDTO coordinates = dto.getCoordinates();
        DragonCaveDTO cave = dto.getCave();
        DragonHeadDTO head = dto.getHead();

        if (coordinates == null || cave == null) return null;

        PersonDTO killer = dto.getKiller();
        LocationDTO location = null;

        if (killer != null) {
            location = killer.getLocation();
            if (location == null) return null;
        }

        return new Dragon(
                dto.getId(),
                dto.getName(),
                new Coordinates(
                        coordinates.getId(),
                        coordinates.getX(),
                        coordinates.getY(),
                        coordinates.getOwnerId(),
                        coordinates.getUpdatedBy(),
                        coordinates.isAllowEditing()
                ),
                dto.getCreationDate(),
                new DragonCave(
                        cave.getId(),
                        cave.getNumberOfTreasures(),
                        cave.getOwnerId(),
                        cave.getUpdatedBy(),
                        cave.isAllowEditing()
                ),
                killer == null ? null : new Person(
                        killer.getId(),
                        killer.getName(),
                        killer.getEyeColor(),
                        killer.getHairColor(),
                        new Location(
                                location.getId(),
                                location.getX(),
                                location.getY(),
                                location.getZ(),
                                location.getOwnerId(),
                                location.getUpdatedBy(),
                                location.isAllowEditing()
                        ),
                        killer.getBirthday(),
                        killer.getHeight(),
                        killer.getOwnerId(),
                        killer.getUpdatedBy(),
                        killer.isAllowEditing()
                ),
                dto.getAge(),
                dto.getDescription(),
                dto.getWingspan(),
                dto.getCharacter(),
                new DragonHead(
                        head.getId(),
                        head.getEyesCount(),
                        head.getToothCount(),
                        head.getOwnerId(),
                        head.getUpdatedBy(),
                        head.isAllowEditing()
                ),
                dto.getOwnerId(),
                dto.getUpdatedBy(),
                dto.isAllowEditing()
        );
    }

    // ---------------- дополнительные функции

    @Transactional
    public int fun1(float eyes, Double tooth) {
        return (int) em.createNativeQuery("SELECT delete_dragons_by_head(:eyes, :tooth)")
                .setParameter("eyes", eyes)
                .setParameter("tooth", tooth)
                .getSingleResult();
    }

    public int fun2(Long wingspan) {
        return (int) em.createNativeQuery("SELECT count_dragons_by_wingspan(:wingspan)")
                .setParameter("wingspan", wingspan)
                .getSingleResult();
    }

    public List<Dragon> fun3(String character) {
        return (List<Dragon>) em.createNativeQuery("SELECT * FROM get_dragons_by_character(:characterValue)", Dragon.class)
                .setParameter("characterValue", character)
                .getResultList();
    }

    public Dragon fun4() {
        return (Dragon) em.createNativeQuery("SELECT * FROM find_dragon_in_deepest_cave()", Dragon.class)
                .getSingleResult();
    }

    @Transactional
    public long fun5(long dragonId) {
        return (long) em.createNativeQuery("SELECT kill_dragon(:dragonId)")
                .setParameter("dragonId", dragonId)
                .getSingleResult();
    }

    // ---------------- вспомогательные к дополнительным

    public List<Dragon> getAliveDragons() {
        return em.createQuery("SELECT e FROM Dragon e WHERE e.killer = NULL", Dragon.class).getResultList();
    }
}
