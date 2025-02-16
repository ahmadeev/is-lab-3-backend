package objects;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "person")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Audited
public class Person {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @NotNull(message = "Поле name не должно быть пустым")
    @NotEmpty
    @Column(name = "name")
    private String name; //Поле не может быть null, Строка не может быть пустой

    @Enumerated(EnumType.STRING)
    @NotNull(message = "Поле eyeColor не должно быть пустым")
    @Column(name = "eye_color")
    private Color eyeColor; //Поле не может быть null

    @Enumerated(EnumType.STRING)
    @Column(name = "hair_color")
    private Color hairColor; //Поле может быть null

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.REMOVE})
    @JoinColumn(name = "location_id", referencedColumnName = "id")
    @NotNull(message = "Поле location не должно быть пустым")
    private Location location; //Поле не может быть null

    @NotNull(message = "Поле birthday не должно быть пустым")
    @Column(name = "birthday")
    private java.time.LocalDate birthday; //Поле не может быть null

    @Positive
    @Column(name = "height")
    private Integer height; //Поле может быть null, Значение поля должно быть больше 0

    // ------------ добавленные

    @Column(name = "owner_id")
    private long ownerId;

    @Column(name = "updated_by")
    private long updatedBy;

    @Column(name = "allow_editing")
    private boolean allowEditing;

    // ------------

    public Person(String name, Color eyeColor, Color hairColor, Location location, LocalDate birthday, Integer height, long ownerId, boolean allowEditing) {
        this.name = name;
        this.eyeColor = eyeColor;
        this.hairColor = hairColor;
        this.location = location;
        this.birthday = birthday;
        this.height = height;
        this.ownerId = ownerId;
        this.allowEditing = allowEditing;
    }

    public Person(String name, Color eyeColor, Color hairColor, Location location, LocalDate birthday, Integer height, boolean allowEditing) {
        this.name = name;
        this.eyeColor = eyeColor;
        this.hairColor = hairColor;
        this.location = location;
        this.birthday = birthday;
        this.height = height;
        this.allowEditing = allowEditing;
    }

    public String toJson() {
        try (Jsonb jsonb = JsonbBuilder.create()) {
            return jsonb.toJson(this);
        } catch (Exception e) {
            e.printStackTrace();
            return "{}";
        }
    }

    @Override
    public String toString() {
        return (id + ". name: " + name + ", eyes:" + eyeColor + ", hair: " + hairColor + ", location: " + location + ", birthday: " + birthday + ", height: " + height);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Person person = (Person) o;
        return ownerId == person.ownerId &&
                Objects.equals(name, person.name) &&
                eyeColor == person.eyeColor &&
                hairColor == person.hairColor &&
                Objects.equals(location, person.location) &&
                Objects.equals(birthday, person.birthday) &&
                Objects.equals(height, person.height);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, eyeColor, hairColor, location, birthday, height, ownerId);
    }
}
