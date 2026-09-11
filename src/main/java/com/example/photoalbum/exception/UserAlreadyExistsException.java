package com.example.photoalbum.exception;

public class UserAlreadyExistsException extends RuntimeException{
  public UserAlreadyExistsException(String message) {
    super(message);
  }
}
