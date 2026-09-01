package com.example.photoalbum.controller;

import com.example.photoalbum.dto.CreateAlbumRequest;
import com.example.photoalbum.model.Album;
import com.example.photoalbum.service.AlbumService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/albums")
public class AlbumController {

  private final AlbumService albumService;

  public AlbumController(AlbumService albumService) {
    this.albumService = albumService;
  }

  @PostMapping
  public ResponseEntity<Album> createAlbum(@RequestBody CreateAlbumRequest request) {
    Album album = albumService.createAlbum(request.name());

    return ResponseEntity.status(HttpStatus.CREATED).body(album);
  }

  @GetMapping
  public List<Album> getAllAlbums() {
    return albumService.getAllAlbums();
  }

  @GetMapping("/{id}")
 public ResponseEntity<Album> getAlbum(@PathVariable String id) {
    Album album = albumService.getAlbum(id);

    if (album == null) {
      return ResponseEntity.notFound().build();
    }

    return ResponseEntity.ok(album);
  }

}
