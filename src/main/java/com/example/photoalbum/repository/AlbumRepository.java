package com.example.photoalbum.repository;

import com.example.photoalbum.model.Album;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;


public interface AlbumRepository extends JpaRepository<Album, String> {
  boolean existsByUser_IdAndName(
      String userId,
      String name
  );

  List<Album> findAllByUser_Id(
      String userId
  );

  Optional<Album> findByIdAndUser_Id(
      String albumId,
      String userId
  );
}
