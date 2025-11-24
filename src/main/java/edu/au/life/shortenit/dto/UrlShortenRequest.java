package edu.au.life.shortenit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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

    private String customAlias;

    private Integer expirationDays; // Optional: number of days until expiration
}