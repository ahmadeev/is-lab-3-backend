package dto;

import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.DefaultValue;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LocationDTO {
    @DefaultValue(value="-1")
    private long id;
    private int x;
    @NotNull
    private Integer y; //Поле не может быть null
    private int z;
}
