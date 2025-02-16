package dto;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.ws.rs.DefaultValue;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import objects.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DragonDTO {
    @DefaultValue(value="-1")
    private long id;
    @NotNull
    private String name; //Поле не может быть null, Строка не может быть пустой
    @NotNull
    private CoordinatesDTO coordinates; //Поле не может быть null
    // вообще не null, но при получении запроса может отсутствовать
    private java.time.ZonedDateTime creationDate; //Поле не может быть null, Значение этого поля должно генерироваться автоматически
    @NotNull
    private DragonCaveDTO cave; //Поле не может быть null
    private PersonDTO killer; //Поле может быть null
    @Positive
    private long age; //Значение поля должно быть больше 0
    private String description; //Поле может быть null
    @NotNull
    @Positive
    private Long wingspan; //Значение поля должно быть больше 0, Поле не может быть null
    private DragonCharacter character; //Поле может быть null
    private DragonHeadDTO head;

    // ------------ добавленные

    @DefaultValue(value="-1")
    private long ownerId;
    @DefaultValue(value="-1")
    private long updatedBy;
    @DefaultValue(value="false")
    private boolean allowEditing;
}
