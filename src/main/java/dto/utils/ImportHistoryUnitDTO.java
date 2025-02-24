package dto.utils;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import objects.utils.ImportStatus;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ImportHistoryUnitDTO {
    @NotNull
    private long id;
    @NotNull
    private ImportStatus status;
    @NotNull
    private long userId;
    private int rowsAdded;
}
