package objects;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.persistence.*;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

@Entity
@Table(name = "dragon_cave")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class DragonCave {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Positive
    @Column(name = "number_of_treasures")
    private float numberOfTreasures; //Значение поля должно быть больше 0

    // ------------ добавленные

    @Column(name = "owner_id")
    private long ownerId;

    // ------------

    public DragonCave(float numberOfTreasures, long ownerId) {
        this.numberOfTreasures = numberOfTreasures;
        this.ownerId = ownerId;
    }

    public DragonCave(float numberOfTreasures) {
        this.numberOfTreasures = numberOfTreasures;
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
        return (id + ". number of treasures: " + numberOfTreasures);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DragonCave that = (DragonCave) o;
        return Float.compare(that.numberOfTreasures, numberOfTreasures) == 0 &&
                ownerId == that.ownerId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(numberOfTreasures, ownerId);
    }
}
