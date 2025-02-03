package objects;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

@Entity
@Table(name = "location")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
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

    // ------------ добавленные

    @Column(name = "owner_id")
    private long ownerId;

    // ------------

    public Location(int x, Integer y, int z, long ownerId) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.ownerId = ownerId;
    }

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Location location = (Location) o;
        return x == location.x &&
                z == location.z &&
                ownerId == location.ownerId &&
                Objects.equals(y, location.y);
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z, ownerId);
    }
}
