package com.example.photoalbum.service;

import com.example.photoalbum.model.Album;
import com.example.photoalbum.repository.AlbumRepository;

import org.springframework.stereotype.Service;
//import java.util.ArrayList;
import java.util.List;
//import java.util.Map;
import java.util.UUID;
//import java.util.concurrent.ConcurrentHashMap;
//
//@Service
//public class AlbumService {
//
//  private final Map<String, Album> albums = new ConcurrentHashMap<>();
//  public Album createAlbum(String name) {
//    String id = UUID.randomUUID().toString();
//    Album album = new Album(id, name);
//    albums.put(id, album);
//    return album;
//  }
//
//  public List<Album> getAllAlbums() {
//    return new ArrayList<>(albums.values());
//  }
//
//  public Album getAlbum(String id) {
//    return albums.get(id);
//  }
//}

@Service
public class AlbumService {
  private final AlbumRepository albumRepository;

  public AlbumService(AlbumRepository albumRepository) {
    this.albumRepository = albumRepository;
  }

  public Album createAlbum(String name) {
    String id = UUID.randomUUID().toString();
    Album album = new Album(id, name);
    return albumRepository.save(album);
  }

  public List<Album> getAllAlbums() {
    return albumRepository.findAll();
  }

  public Album getAlbum(String id) {
    return albumRepository.findById(id).orElse(null);
  }
}
