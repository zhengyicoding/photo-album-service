package com.example.photoalbum.service;

import com.example.photoalbum.exception.AlbumAlreadyExistsException;
import com.example.photoalbum.exception.UserNotFoundException;
import com.example.photoalbum.model.Album;
import com.example.photoalbum.model.User;
import com.example.photoalbum.repository.AlbumRepository;
import com.example.photoalbum.repository.UserRepository;

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
  private final UserRepository userRepository;

  public AlbumService(
      AlbumRepository albumRepository,
      UserRepository userRepository) {
    this.albumRepository = albumRepository;
    this.userRepository = userRepository;
  }

  public Album createAlbum(
      String userId,
      String name) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() ->
                new UserNotFoundException("User not found" + userId));
    if (albumRepository.existsByUser_IdAndName(userId, name)) {
      throw new AlbumAlreadyExistsException("Album name already exists");
    }
    String albumId = UUID.randomUUID().toString();
    Album album = new Album(albumId, name, user);
    return albumRepository.save(album);
  }

  public List<Album> getAllAlbums(String userId) {
    return albumRepository.findAllByUser_Id(userId);
  }

  public Album getAlbum(String userId, String albumId) {
    return albumRepository.findByIdAndUser_Id(albumId, userId).orElse(null);
  }
}
