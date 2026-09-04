package com.example.photoalbum.repository;

import com.example.photoalbum.model.Album;
import org.springframework.data.jpa.repository.JpaRepository;


public interface AlbumRepository extends JpaRepository<Album, String> {
}
