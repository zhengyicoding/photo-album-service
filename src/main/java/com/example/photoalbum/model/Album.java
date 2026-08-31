package com.example.photoalbum.model;

public class Album {
  private String id;
  private String name;

  public Album(String id, String name) {
    this.id = id;
    this.name = name;
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }
}
