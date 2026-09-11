package com.example.photoalbum.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAlbumRequest(
    @NotBlank(message = "Album name is required")
    @Size(max = 100, message = "Album name must be at most 100 characters") String name) {
}
