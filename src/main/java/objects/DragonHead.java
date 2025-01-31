package objects;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "dragon_head")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class DragonHead {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "eyes_count")
    private float eyesCount;

    @NotNull(message = "Поле tooth_count не должно быть пустым")
    @Column(name = "tooth_count")
    private Double toothCount; //Поле не может быть null

    public DragonHead(float eyesCount, Double toothCount) {
        this.eyesCount = eyesCount;
        this.toothCount = toothCount;
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
}
