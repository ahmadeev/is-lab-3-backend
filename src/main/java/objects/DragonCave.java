package objects;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.persistence.*;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.util.Objects;

@Entity
@Table(name = "dragon_cave")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Audited
public class DragonCave {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cave_seq")
    @SequenceGenerator(name = "cave_seq", sequenceName = "cave_sequence", allocationSize = 50)
    private long id;

    @Positive
    @Column(name = "number_of_treasures")
    private float numberOfTreasures; //Значение поля должно быть больше 0

    // ------------ добавленные

    @Column(name = "owner_id")
    private long ownerId;

    @Column(name = "updated_by")
    private long updatedBy;

    @Column(name = "allow_editing")
    private boolean allowEditing;

    // ------------

    public DragonCave(float numberOfTreasures, long ownerId, boolean allowEditing) {
        this.numberOfTreasures = numberOfTreasures;
        this.ownerId = ownerId;
        this.allowEditing = allowEditing;
    }

    public DragonCave(float numberOfTreasures, boolean allowEditing) {
        this.numberOfTreasures = numberOfTreasures;
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

    public boolean isValid() {
        DragonCave c = this;

        return (
            c.numberOfTreasures > 0
        );
    }
}
