package com.example.photoalbum.model;

import jakarta.persistence.*;


@Entity
@Table(
    name = "albums",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_album_user_name",
            columnNames = {"user_id", "name"}
        )
    }
)
public class Album {
  @Id
  private String id;
  @Column(nullable = false)
  private String name;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  protected Album() {

  }

  public Album(String id, String name, User user) {
    this.id = id;
    this.name = name;
    this.user = user;
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public User getUser() {
    return user;
  }
}
