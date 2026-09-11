package com.example.photoalbum.service;

import com.example.photoalbum.exception.UserAlreadyExistsException;
import com.example.photoalbum.model.User;
import com.example.photoalbum.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserService {
  private final UserRepository userRepository;

  public UserService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  public User createUser(String email) {
    if (userRepository.existsByEmail(email)) {
      throw new UserAlreadyExistsException("User already exists: " + email);
    }
    User user = new User(UUID.randomUUID().toString(),
        email);
    return userRepository.save(user);
  }
}
