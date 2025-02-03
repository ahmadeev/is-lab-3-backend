package objects;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.ZonedDateTime;
import java.util.Objects;

@Entity
@Table(name = "dragon")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class Dragon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id; //Значение поля должно быть больше 0, Значение этого поля должно быть уникальным, Значение этого поля должно генерироваться автоматически

    @NotNull(message = "Поле name не должно быть пустым")
    @Column(name = "name")
    private String name; //Поле не может быть null, Строка не может быть пустой

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "coordinates_id", referencedColumnName = "id")
    @NotNull(message = "Поле класса Coordinates не должно быть пустым")
    private Coordinates coordinates; //Поле не может быть null

    @NotNull(message = "Поле creationDate не должно быть пустым")
    @Column(name = "creation_date")
    private java.time.ZonedDateTime creationDate; //Поле не может быть null, Значение этого поля должно генерироваться автоматически

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "dragon_cave_id", referencedColumnName = "id")
    @NotNull(message = "Поле класса DragonCave не должно быть пустым")
    private DragonCave cave; //Поле не может быть null

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "person_id", referencedColumnName = "id")
    private Person killer; //Поле может быть null

    @Positive
    @Column(name = "age")
    private long age; //Значение поля должно быть больше 0

    @Column(name = "description")
    private String description; //Поле может быть null

    @NotNull(message = "Поле wingspan не должно быть пустым")
    @Positive
    @Column(name = "wingspan")
    private Long wingspan; //Значение поля должно быть больше 0, Поле не может быть null

    @Enumerated(EnumType.STRING)
    @Column(name = "character")
    private DragonCharacter character; //Поле может быть null

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "dragon_head_id", referencedColumnName = "id")
    private DragonHead head;

    // ------------ добавленные

    @Column(name = "owner_id")
    private long ownerId;

    // ------------

    public Dragon(String name, Coordinates coordinates, DragonCave cave, Person killer, long age, String description, Long wingspan, DragonCharacter character, DragonHead head, long ownerId) {
        this.name = name;
        this.coordinates = coordinates;
        this.cave = cave;
        this.killer = killer;
        this.age = age;
        this.description = description;
        this.wingspan = wingspan;
        this.character = character;
        this.head = head;
        this.ownerId = ownerId;
    }

    public Dragon(String name, Coordinates coordinates, DragonCave cave, Person killer, long age, String description, Long wingspan, DragonCharacter character, DragonHead head) {
        this.name = name;
        this.coordinates = coordinates;
        this.cave = cave;
        this.killer = killer;
        this.age = age;
        this.description = description;
        this.wingspan = wingspan;
        this.character = character;
        this.head = head;
    }

    @PrePersist
    protected void onCreate() {
        creationDate = ZonedDateTime.now(); // Установка текущей даты при создании
    }

    public String toJson() {
        try (Jsonb jsonb = JsonbBuilder.create()) {
            return jsonb.toJson(this);
        } catch (Exception e) {
            e.printStackTrace();
            return "{}";
        }
    }

    // toString()

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Dragon dragon = (Dragon) o;
        return ownerId == dragon.ownerId &&
                age == dragon.age &&
                Objects.equals(name, dragon.name) &&
                Objects.equals(coordinates, dragon.coordinates) &&
                Objects.equals(cave, dragon.cave) &&
                Objects.equals(killer, dragon.killer) &&
                Objects.equals(description, dragon.description) &&
                Objects.equals(wingspan, dragon.wingspan) &&
                character == dragon.character &&
                Objects.equals(head, dragon.head);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, coordinates, cave, killer, age, description, wingspan, character, head, ownerId);
    }
}
