package com.example.photoalbum.repository;

import com.example.photoalbum.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository
    extends JpaRepository<User, String> {
  boolean existsByEmail(String email);
}