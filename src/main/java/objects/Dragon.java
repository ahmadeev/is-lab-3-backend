package objects;

import jakarta.enterprise.inject.Model;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.time.ZonedDateTime;
import java.util.Objects;

@Entity
@Table(name = "dragon")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Audited
public class Dragon {
    // такой тип генерации, потому что IDENTITY требует следующий айди после инсерта (а из-за батчей их кратно меньше)
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "dragon_seq") // вместо @GeneratedValue(strategy = GenerationType.IDENTITY)
    @SequenceGenerator(name = "dragon_seq", sequenceName = "dragon_sequence", allocationSize = 50)
    private long id; //Значение поля должно быть больше 0, Значение этого поля должно быть уникальным, Значение этого поля должно генерироваться автоматически

    @NotNull(message = "Поле name не должно быть пустым")
    @Column(name = "name")
    private String name; //Поле не может быть null, Строка не может быть пустой

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.REMOVE})
    @JoinColumn(name = "coordinates_id", referencedColumnName = "id")
    @NotNull(message = "Поле класса Coordinates не должно быть пустым")
    private Coordinates coordinates; //Поле не может быть null

    @NotNull(message = "Поле creationDate не должно быть пустым")
    @Column(name = "creation_date")
    private java.time.ZonedDateTime creationDate; //Поле не может быть null, Значение этого поля должно генерироваться автоматически

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.REMOVE})
    @JoinColumn(name = "dragon_cave_id", referencedColumnName = "id")
    @NotNull(message = "Поле класса DragonCave не должно быть пустым")
    private DragonCave cave; //Поле не может быть null

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.REMOVE})
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

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.REMOVE})
    @JoinColumn(name = "dragon_head_id", referencedColumnName = "id")
    private DragonHead head;

    // ------------ добавленные

    @Column(name = "owner_id")
    private long ownerId;
    @Column(name = "updated_by")
    private long updatedBy;

    @Column(name = "allow_editing")
    private boolean allowEditing;

//    @Column(name = "created_at")
//    private LocalDateTime createdAt;
//    @Column(name = "updated_at")
//    private LocalDateTime updatedAt;
//    @Column(name = "created_by")
//    private long createdBy;
//    @Column(name = "updated_by")
//    private long updatedBy;

    // ------------

    // + owner_id // TODO: НО БЕЗ updatedBy ???
    public Dragon(
            String name,
            Coordinates coordinates,
            DragonCave cave,
            Person killer,
            long age,
            String description,
            Long wingspan,
            DragonCharacter character,
            DragonHead head,
            long ownerId,
            boolean allowEditing
    ) {
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
        this.allowEditing = allowEditing;
    }

    public Dragon(
            String name,
            Coordinates coordinates,
            DragonCave cave,
            Person killer,
            long age,
            String description,
            Long wingspan,
            DragonCharacter character,
            DragonHead head,
            boolean allowEditing
    ) {
        this.name = name;
        this.coordinates = coordinates;
        this.cave = cave;
        this.killer = killer;
        this.age = age;
        this.description = description;
        this.wingspan = wingspan;
        this.character = character;
        this.head = head;
        this.allowEditing = allowEditing;
    }

    @PrePersist
    protected void onCreate() {
        creationDate = ZonedDateTime.now(); // Установка текущей даты при создании
//        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
//        updatedAt = LocalDateTime.now();
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

    public boolean isValid() {
        Dragon d = this;

        return (
                !(Objects.isNull(d.name) || d.name.isEmpty()) &&
                d.coordinates.isValid() &&
                d.cave.isValid() &&
                (d.killer.isValid() || Objects.isNull(d.killer)) &&
                d.age > 0 &&
                d.wingspan > 0 &&
                d.head.isValid()
        );
    }
}
