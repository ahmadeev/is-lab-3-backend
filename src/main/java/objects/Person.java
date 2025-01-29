package objects;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "person")
@Getter
@Setter
@NoArgsConstructor
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

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "location_id", referencedColumnName = "id")
    @NotNull(message = "Поле location не должно быть пустым")
    private Location location; //Поле не может быть null

    @NotNull(message = "Поле birthday не должно быть пустым")
    @Column(name = "birthday")
    private java.time.LocalDate birthday; //Поле не может быть null

    @Positive
    @Column(name = "height")
    private Integer height; //Поле может быть null, Значение поля должно быть больше 0

    public Person(String name, Color eyeColor, Color hairColor, Location location, LocalDate birthday, Integer height) {
        this.name = name;
        this.eyeColor = eyeColor;
        this.hairColor = hairColor;
        this.location = location;
        this.birthday = birthday;
        this.height = height;
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
}
