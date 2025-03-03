package objects;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.util.Objects;

@Entity
@Table(name = "dragon_head")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Audited
public class DragonHead {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "head_seq")
    @SequenceGenerator(name = "head_seq", sequenceName = "head_sequence", allocationSize = 50)
    private long id;

    @Column(name = "eyes_count")
    private float eyesCount;

    @NotNull(message = "Поле tooth_count не должно быть пустым")
    @Column(name = "tooth_count")
    private Double toothCount; //Поле не может быть null

    // ------------ добавленные

    @Column(name = "owner_id")
    private long ownerId;

    @Column(name = "updated_by")
    private long updatedBy;

    @Column(name = "allow_editing")
    private boolean allowEditing;

    // ------------

    public DragonHead(float eyesCount, Double toothCount, long ownerId, boolean allowEditing) {
        this.eyesCount = eyesCount;
        this.toothCount = toothCount;
        this.ownerId = ownerId;
        this.allowEditing = allowEditing;
    }

    public DragonHead(float eyesCount, Double toothCount, boolean allowEditing) {
        this.eyesCount = eyesCount;
        this.toothCount = toothCount;
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
        return (id + ". eyes: " + eyesCount + ", tooth: " + toothCount);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DragonHead that = (DragonHead) o;
        return Float.compare(that.eyesCount, eyesCount) == 0 &&
                ownerId == that.ownerId &&
                Objects.equals(toothCount, that.toothCount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eyesCount, toothCount, ownerId);
    }

    public boolean isValid() {
        DragonHead dh = this;

        return (
            !Objects.isNull(dh.toothCount)
        );
    }
}
