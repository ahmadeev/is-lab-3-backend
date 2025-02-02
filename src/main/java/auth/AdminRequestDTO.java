package auth;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class AdminRequestDTO {
    @NotNull
    private long id;
    @NotNull
    private String name;
}

