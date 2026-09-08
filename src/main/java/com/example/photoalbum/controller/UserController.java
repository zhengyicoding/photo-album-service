package com.example.photoalbum.controller;

import com.example.photoalbum.dto.CreateUserRequest;
import com.example.photoalbum.model.User;
import com.example.photoalbum.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {
  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  @PostMapping
  public ResponseEntity<User> createUser(
      @RequestBody CreateUserRequest request) {
    User user = userService.createUser(request.email());

    return ResponseEntity.status(HttpStatus.CREATED).body(user);
  }
}
