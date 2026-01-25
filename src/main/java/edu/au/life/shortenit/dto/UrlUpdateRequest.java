package edu.au.life.shortenit.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UrlUpdateRequest {

    @Size(min = 1, max = 200, message = "Title must be between 1 and 200 characters")
    private String title;

    @Min(value = 1, message = "Expiration days must be at least 1")
    private Integer expirationDays;

    private Boolean clearExpiration;

    @Size(min = 1, max = 50, message = "Code must be between 1 and 50 characters")
    @jakarta.validation.constraints.Pattern(
            regexp = "^[a-zA-Z0-9-]*$",
            message = "Code can only contain letters, numbers, and hyphens"
    )
    private String code;

    private Boolean isActive;
}
