package com.example.photoalbum.model;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {

  @Id
  private String id;

  @Column(nullable = false, unique = true)
  private String email;

  protected User() {
  }

  public User(String id, String email) {
    this.id = id;
    this.email = email;
  }

  public String getId() {
    return id;
  }

  public String getEmail() {
    return email;
  }
}