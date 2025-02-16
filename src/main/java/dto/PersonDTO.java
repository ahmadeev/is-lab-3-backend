package dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.ws.rs.DefaultValue;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import objects.Color;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PersonDTO {
    @DefaultValue(value="-1")
    private long id;
    @NotNull
    @NotEmpty
    private String name; //Поле не может быть null, Строка не может быть пустой
    @NotNull
    private Color eyeColor; //Поле не может быть null
    private Color hairColor; //Поле может быть null
    @NotNull
    private LocationDTO location; //Поле не может быть null
    @NotNull
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private java.time.LocalDate birthday; //Поле не может быть null
    @Positive
    private Integer height; //Поле может быть null, Значение поля должно быть больше 0

    // ------------ добавленные

    @DefaultValue(value="-1")
    private long ownerId;
    @DefaultValue(value="-1")
    private long updatedBy;
    @DefaultValue(value="false")
    private boolean allowEditing;
}
