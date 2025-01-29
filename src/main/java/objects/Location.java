package objects;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "location")
@Getter @Setter
@NoArgsConstructor
public class Location {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "x")
    private int x;

    @NotNull(message = "Поле y не должно быть пустым")
    @Column(name = "y")
    private Integer y; //Поле не может быть null

    @Column(name = "z")
    private int z;

    public Location(int x, Integer y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
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
        return (id + ". x: " + x + ", y: " + y + ", z: " + z);
    }
}
