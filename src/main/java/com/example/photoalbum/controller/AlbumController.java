package com.example.photoalbum.controller;

import com.example.photoalbum.dto.CreateAlbumRequest;
import com.example.photoalbum.model.Album;
import com.example.photoalbum.service.AlbumService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("users/{userId}/albums")
public class AlbumController {

  private final AlbumService albumService;

  public AlbumController(AlbumService albumService) {
    this.albumService = albumService;
  }

  @PostMapping
  public ResponseEntity<Album> createAlbum(
      @PathVariable String userId,
      @Valid @RequestBody CreateAlbumRequest request
  ) {
    Album album = albumService.createAlbum(userId, request.name());

    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(album);
  }

  @GetMapping
  public List<Album> getAllAlbums(
      @PathVariable String userId
  ) {
    return albumService.getAllAlbums(userId);
  }

  @GetMapping("/{albumId}")
  public ResponseEntity<Album> getAlbum(
      @PathVariable String userId,
      @PathVariable String albumId) {
    Album album = albumService.getAlbum(userId, albumId);
    if (album == null) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(album);
  }
}
