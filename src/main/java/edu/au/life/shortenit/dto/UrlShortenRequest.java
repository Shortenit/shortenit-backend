package edu.au.life.shortenit.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UrlShortenRequest {

    @NotBlank(message = "URL cannot be empty")
    @Pattern(regexp = "^(http|https)://.*", message = "URL must start with http:// or https://")
    private String originalUrl;

    @NotBlank(message = "Title is required")
    @Size(min = 1, max = 200, message = "Title must be between 1 and 200 characters")
    private String title;

    @Size(min = 1, max = 50, message = "Code must be between 1 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9-]*$", message = "Code can only contain letters, numbers, and hyphens")
    private String code;

    @Min(value = 1, message = "Expiration days must be at least 1")
    private Integer expirationDays;
}