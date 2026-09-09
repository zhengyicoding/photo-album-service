package com.example.photoalbum.exception;

public class AlbumAlreadyExistsException extends RuntimeException {
  public AlbumAlreadyExistsException(String message) {
    super(message);
  }
}
