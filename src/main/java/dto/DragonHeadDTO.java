package dto;

import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.DefaultValue;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DragonHeadDTO {
    @DefaultValue(value="-1")
    private long id;
    private float eyesCount;
    @NotNull
    private Double toothCount; //Поле не может быть null

    // ------------ добавленные

    @DefaultValue(value="-1")
    private long ownerId;
    @DefaultValue(value="-1")
    private long updatedBy;
    @DefaultValue(value="false")
    private boolean allowEditing;
}
