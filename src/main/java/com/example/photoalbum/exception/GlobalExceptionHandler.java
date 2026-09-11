package com.example.photoalbum.exception;

import com.example.photoalbum.dto.ApiError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(AlbumAlreadyExistsException.class)
  public ResponseEntity<ApiError> handleAlbumAlreadyExists(
      AlbumAlreadyExistsException ex) {
    ApiError error = new ApiError("ALBUM_ALREADY_EXISTS", ex.getMessage());
    return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
  }

  @ExceptionHandler(UserNotFoundException.class)
  public ResponseEntity<ApiError> handleUserNotFound(UserNotFoundException ex) {
    ApiError error = new ApiError("USER_NOT_FOUND", ex.getMessage());
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
  }

  @ExceptionHandler(UserAlreadyExistsException.class)
  public ResponseEntity<ApiError> handleUserAlreadyExists(UserAlreadyExistsException ex) {
    ApiError error = new ApiError("USER_ALREADY_EXISTS", ex.getMessage());
    return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
  }
}
